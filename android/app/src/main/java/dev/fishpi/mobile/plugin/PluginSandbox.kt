package dev.fishpi.mobile.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

internal class PluginSandbox(
    private val context: Context,
    val fileName: String,
    val header: PluginHeader,
    private val script: String,
) {
    val id: String = fileName
    lateinit var bridge: PluginBridge
        private set
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        val wv: WebView
        try {
            wv = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        try { injectPlugin() } catch (_: Exception) {}
                    }
                }
            }
            webView = wv
        } catch (_: Exception) {
            return
        }

        val evalFn: (String) -> Unit = { js ->
            mainHandler.post {
                try { wv.evaluateJavascript(js, null) } catch (_: Exception) {}
            }
        }

        bridge = PluginBridge(
            pluginId = id,
            evalJs = evalFn,
            dispatch = PluginManager.get()::dispatch,
        )

        try {
            wv.addJavascriptInterface(bridge, "__bridge")
            wv.loadDataWithBaseURL("https://fishpi.plugin/", SHELL_HTML, "text/html", "UTF-8", null)
        } catch (_: Exception) {}
    }

    private fun injectContext() {
        val userName = PluginManager.get().userName
        val apiKey = PluginManager.get().apiKey
        webView?.evaluateJavascript(
            "var userName='${userName.replace("'","\\'")}';" +
                "var apiKey='${apiKey.replace("'","\\'")}';" +
                "var userAvatarURL='';",
            null,
        )
    }

    private fun injectPlugin() {
        injectContext()
        webView?.evaluateJavascript(
            "try{(function(){$script})();__bridge.log('loaded');}catch(e){__bridge.log('load error: '+e.message);}",
            null,
        )
    }

    fun applySendHook(text: String, onResult: (String) -> Unit) {
        val json = org.json.JSONObject().put("t", text).toString()
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
        val js = "fishpi.applySendHook(JSON.parse('$escaped').t)"
        try {
            webView?.evaluateJavascript(js) { raw ->
                val unquoted = parseJsString(raw ?: "\"\"")
                onResult(unquoted)
            }
        } catch (_: Exception) { onResult(text) }
    }

    private fun parseJsString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed == "null" || trimmed == "undefined" || trimmed.isEmpty()) return ""
        return try {
            org.json.JSONObject("{\"v\":$trimmed}").getString("v")
        } catch (_: Exception) { trimmed }
    }

    fun destroy() {
        try { webView?.removeJavascriptInterface("__bridge"); webView?.destroy() } catch (_: Exception) {}
        webView = null
    }

    companion object {
        val SHELL_HTML = """
<html><body><script>
var _handlers={};
var _hooks={};
function _emit(event,dataJson){
    var data=dataJson?JSON.parse(dataJson):null;
    if(data&&event==='message'){
        var hooks=_hooks[event];if(hooks)hooks.forEach(function(fn){fn(data)});
        if(data.filtered)return;
    }
    var arr=_handlers[event];if(!arr)return;
    arr.forEach(function(fn){fn(data)});
}
function on(event,fn){_handlers[event]=_handlers[event]||[];_handlers[event].push(fn);}
function off(event,fn){var a=_handlers[event];if(a)_handlers[event]=a.filter(function(f){return f!==fn;});}
var _sendText='';
var fishpi={hook:function(name,fn){_hooks[name]=_hooks[name]||[];_hooks[name].push(fn);},
applySendHook:function(text){var t=text;var hs=_hooks.sendMessage;if(hs)hs.forEach(function(h){t=h(t)});return t;},
call:function(method,params){
    var r=__bridge.call(method,JSON.stringify(params||{}));
    return Promise.resolve(JSON.parse(r));
}};
fishpi.toolbar={
    register:function(entry){return fishpi.call('toolbar.register',entry||{});},
    unregister:function(id){return fishpi.call('toolbar.unregister',{id:id});},
    clear:function(){return fishpi.call('toolbar.clear',{});}
};
fishpi.chat={
    setClientType:function(client,version){return fishpi.call('chat.setClientType',{client:client,version:version});},
    clearClientType:function(){return fishpi.call('chat.clearClientType',{});}
};
var storage={
    get:function(key,def){
        var v=__bridge.getStorage(key,'__fishpi_null__');
        if(v==='__fishpi_null__'){__bridge.setStorage(key,JSON.stringify(def));return def;}
        try{return JSON.parse(v);}catch(e){return v;}
    },
    set:function(key,val){__bridge.setStorage(key,JSON.stringify(val));}
};
var ui={
    toast:function(text){__bridge.call('systemMessage',JSON.stringify({text:text}));},
    notify:function(textOrOptions,type){
        var payload=typeof textOrOptions==='object'&&textOrOptions!==null
            ? Object.assign({},textOrOptions)
            : {text:String(textOrOptions||''),type:type||'info'};
        return fishpi.call('app.notify',payload);
    }
};
fishpi.ui=(function(){
    var callbacks={};
    function call(action,args){return fishpi.call(action,args||{});}
    function page(container,title){
        var nodes=[];
        var api={
            id:'ui-'+Math.random().toString(36).slice(2),
            container:container,
            title:title||'插件',
            nodes:nodes,
            open:function(){return call('ui.open',{id:this.id,container:this.container,title:this.title,nodes:this.nodes});},
            close:function(){return call('ui.close',{});},
            clear:function(){nodes.length=0;return call('ui.clear',{});},
            update:function(){return call('ui.update',{id:this.id,container:this.container,title:this.title,nodes:this.nodes});},
            push:function(n){nodes.push(n);return this;},
            text:function(text,opts){opts=opts||{};return this.push(Object.assign({type:'text',text:String(text||'')},opts));},
            markdown:function(text,opts){return this.push(Object.assign({type:'markdown',text:String(text||'')},opts||{}));},
            image:function(url,opts){return this.push(Object.assign({type:'image',url:String(url||'')},opts||{}));},
            divider:function(){return this.push({type:'divider'});},
            space:function(height){return this.push({type:'space',height:height||12});},
            json:function(data){return this.push({type:'json',data:data});},
            card:function(opts){return this.push(Object.assign({type:'card'},opts||{}));},
            section:function(title,children){return this.push({type:'section',title:title||'',children:children||[]});},
            row:function(children){return this.push({type:'row',children:children||[]});},
            columns:function(children){return this.push({type:'columns',children:children||[]});},
            tabs:function(tabs){return this.push({type:'tabs',tabs:tabs||[]});},
            input:function(name,label,opts){return this.push(Object.assign({type:'input',name:name,label:label},opts||{}));},
            textarea:function(name,label,opts){return this.push(Object.assign({type:'textarea',name:name,label:label},opts||{}));},
            number:function(name,label,opts){return this.push(Object.assign({type:'number',name:name,label:label},opts||{}));},
            switch:function(name,label,opts){return this.push(Object.assign({type:'switch',name:name,label:label},opts||{}));},
            select:function(name,label,options,opts){return this.push(Object.assign({type:'select',name:name,label:label,options:options||[]},opts||{}));},
            chips:function(name,options,opts){return this.push(Object.assign({type:'chips',name:name,options:options||[]},opts||{}));},
            slider:function(name,label,opts){return this.push(Object.assign({type:'slider',name:name,label:label},opts||{}));},
            loading:function(text){return this.push({type:'loading',text:text||'加载中...'});},
            error:function(text){return this.push({type:'error',text:text||'加载失败'});},
            empty:function(text){return this.push({type:'empty',text:text||'暂无内容'});},
            list:function(items){return this.push({type:'list',items:items||[]});},
            table:function(headers,rows){return this.push({type:'table',headers:headers||[],rows:rows||[]});},
            stat:function(label,value,detail){return this.push({type:'stat',label:label,value:String(value||''),detail:detail||''});},
            userCard:function(opts){return this.push(Object.assign({type:'userCard'},opts||{}));},
            articleCard:function(opts){return this.push(Object.assign({type:'articleCard'},opts||{}));},
            actionBar:function(actions){return this.push({type:'actionBar',actions:(actions||[]).map(bindAction)});},
            button:function(label,fn,opts){opts=opts||{};var id=opts.id||('action-'+Math.random().toString(36).slice(2));if(fn)callbacks[id]=fn;return this.push(Object.assign({type:'button',label:label,actionId:id},opts));}
        };
        return api;
    }
    function bindAction(action){action=action||{};if(action.onClick){var id=action.id||('action-'+Math.random().toString(36).slice(2));callbacks[id]=action.onClick;action.actionId=id;}return action;}
    on('uiAction',function(e){var fn=callbacks[e&&e.actionId];if(fn)try{fn(e.values||{},e);}catch(err){log('ui callback error: '+err.message);}});
    return {dialog:function(title){return page('dialog',title);},page:function(title){return page('page',title);},_callbacks:callbacks};
})();
ui.dialog=fishpi.ui.dialog;
ui.page=fishpi.ui.page;
function log(msg){__bridge.log('[plugin] '+msg);}
window.onerror=function(m,s,l){__bridge.log('ERR:'+m+' at '+s+':'+l);return true;};
</script></body></html>
""".trimIndent()
    }
}
