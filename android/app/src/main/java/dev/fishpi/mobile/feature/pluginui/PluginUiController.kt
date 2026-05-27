package dev.fishpi.mobile.feature.pluginui

import dev.fishpi.mobile.core.ui.UiController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class PluginUiController private constructor() : UiController<PluginUiState, PluginUiAction> {
    private val _state = MutableStateFlow(PluginUiState())
    override val state: StateFlow<PluginUiState> = _state

    private val _effects = MutableSharedFlow<PluginUiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<PluginUiEffect> = _effects.asSharedFlow()

    private var actionHandler: ((pluginId: String, actionId: String, values: JSONObject) -> Unit)? = null

    override fun dispatch(action: PluginUiAction) {
        runCatching {
            when (action) {
                is PluginUiAction.Open -> open(action.pluginId, action.payload)
                is PluginUiAction.Update -> update(action.pluginId, action.payload)
                is PluginUiAction.Close -> closeInternal(action.pluginId)
                is PluginUiAction.Clear -> clearInternal(action.pluginId)
                is PluginUiAction.TriggerAction -> trigger(action.actionId, action.nodeId)
                is PluginUiAction.ChangeText -> changeForm(action.name, PluginFormValue.Text(action.value))
                is PluginUiAction.ChangeNumber -> changeForm(action.name, PluginFormValue.Number(action.value))
                is PluginUiAction.ChangeBool -> changeForm(action.name, PluginFormValue.Bool(action.value))
                is PluginUiAction.ChangeStrings -> changeForm(action.name, PluginFormValue.Strings(action.value))
                PluginUiAction.ClearError -> _state.update { it.copy(lastError = null) }
            }
        }.onFailure {
            val message = it.message ?: "插件 UI 操作失败"
            _state.update { state -> state.copy(lastError = message) }
            _effects.tryEmit(PluginUiEffect.ShowError(message))
        }
    }

    fun setActionHandler(handler: (pluginId: String, actionId: String, values: JSONObject) -> Unit) {
        actionHandler = handler
    }

    fun open(pluginId: String, payload: JSONObject): JSONObject {
        dispatch(PluginUiAction.Open(pluginId, payload))
        return JSONObject().put("ok", true)
    }

    fun update(pluginId: String, payload: JSONObject): JSONObject {
        dispatch(PluginUiAction.Update(pluginId, payload))
        return JSONObject().put("ok", true)
    }

    fun close(pluginId: String? = null): JSONObject {
        dispatch(PluginUiAction.Close(pluginId))
        return JSONObject().put("ok", true)
    }

    fun clear(pluginId: String): JSONObject {
        dispatch(PluginUiAction.Clear(pluginId))
        return JSONObject().put("ok", true)
    }

    private fun open(pluginId: String, payload: JSONObject, direct: Boolean = true) {
        val next = PluginUiMapper.document(pluginId, payload).copy(open = true)
        _state.update { state ->
            val current = state.current
            val stack = if (next.container == PluginUiContainerType.Page && current?.open == true) {
                state.backStack + current
            } else {
                state.backStack
            }
            state.copy(current = next, backStack = stack, form = initialForm(next), lastError = null)
        }
    }

    private fun update(pluginId: String, payload: JSONObject, direct: Boolean = true) {
        val current = _state.value.current
        if (current == null || current.pluginId != pluginId) {
            open(pluginId, payload, direct = direct)
            return
        }
        val partialNodes = payload.optJSONArray("nodes") ?: payload.optJSONArray("children")
        val next = current.copy(
            title = payload.optString("title").ifBlank { current.title },
            nodes = partialNodes?.let { PluginUiMapper.nodes(it) } ?: current.nodes,
            error = payload.optString("error").takeIf { it.isNotBlank() },
        )
        _state.update { it.copy(current = next, form = mergeForm(it.form, next), lastError = null) }
    }

    private fun closeInternal(pluginId: String?) {
        _state.update { state ->
            val current = state.current
            if (pluginId != null && current?.pluginId != pluginId) return@update state
            val previous = state.backStack.lastOrNull()
            state.copy(
                current = previous,
                backStack = if (previous == null) emptyList() else state.backStack.dropLast(1),
                form = previous?.let(::initialForm) ?: PluginFormState(),
            )
        }
    }

    private fun clearInternal(pluginId: String) {
        _state.update { state ->
            val current = state.current
            if (current?.pluginId != pluginId) return@update state
            state.copy(current = current.copy(nodes = emptyList(), error = null), form = PluginFormState())
        }
    }

    private fun trigger(actionId: String, nodeId: String) {
        val current = _state.value.current ?: return
        if (actionId.isBlank()) return
        actionHandler?.invoke(current.pluginId, actionId, formValuesJson(_state.value.form, nodeId))
    }

    private fun changeForm(name: String, value: PluginFormValue) {
        if (name.isBlank()) return
        _state.update { it.copy(form = it.form.put(name, value)) }
    }

    private fun initialForm(document: PluginUiDocument): PluginFormState {
        var form = PluginFormState()
        document.nodes.forEachNode { node ->
            form = when (node) {
                is PluginUiNode.Input -> form.put(node.name, PluginFormValue.Text(node.value))
                is PluginUiNode.Number -> form.put(node.name, PluginFormValue.Number(node.value))
                is PluginUiNode.Switch -> form.put(node.name, PluginFormValue.Bool(node.checked))
                is PluginUiNode.Select -> form.put(node.name, PluginFormValue.Text(node.value))
                is PluginUiNode.Chips -> form.put(node.name, PluginFormValue.Strings(node.values))
                is PluginUiNode.Slider -> form.put(node.name, PluginFormValue.Number(node.value.toDouble()))
                else -> form
            }
        }
        return form
    }

    private fun mergeForm(current: PluginFormState, document: PluginUiDocument): PluginFormState {
        val defaults = initialForm(document)
        return defaults.copy(values = defaults.values + current.values)
    }

    private fun formValuesJson(form: PluginFormState, nodeId: String): JSONObject {
        val values = JSONObject()
        form.values.forEach { (name, value) ->
            when (value) {
                is PluginFormValue.Text -> values.put(name, value.value)
                is PluginFormValue.Number -> values.put(name, value.value)
                is PluginFormValue.Bool -> values.put(name, value.value)
                is PluginFormValue.Strings -> values.put(name, JSONArray(value.value))
            }
        }
        return JSONObject()
            .put("nodeId", nodeId)
            .put("values", values)
    }

    private fun List<PluginUiNode>.forEachNode(block: (PluginUiNode) -> Unit) {
        forEach { node ->
            block(node)
            when (node) {
                is PluginUiNode.Card -> node.children.forEachNode(block)
                is PluginUiNode.Section -> node.children.forEachNode(block)
                is PluginUiNode.Row -> node.children.forEachNode(block)
                is PluginUiNode.Columns -> node.children.forEachNode(block)
                is PluginUiNode.Tabs -> node.tabs.forEach { it.children.forEachNode(block) }
                else -> Unit
            }
        }
    }

    companion object {
        private val INSTANCE = PluginUiController()
        fun get(): PluginUiController = INSTANCE
    }
}
