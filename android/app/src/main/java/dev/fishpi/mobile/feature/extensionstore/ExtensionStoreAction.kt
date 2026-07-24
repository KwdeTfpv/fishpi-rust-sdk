package dev.fishpi.mobile.feature.extensionstore

import dev.fishpi.mobile.data.ExtensionStoreItem
import dev.fishpi.mobile.data.ExtensionStoreUploadRequest

internal sealed interface ExtensionStoreAction {
    data object Initialize : ExtensionStoreAction
    data object Refresh : ExtensionStoreAction
    data class ChangeFilter(val filter: StoreFilter) : ExtensionStoreAction
    data class ChangeQuery(val query: String) : ExtensionStoreAction
    data class Purchase(val item: ExtensionStoreItem) : ExtensionStoreAction
    data class Upload(val request: ExtensionStoreUploadRequest) : ExtensionStoreAction
    data object LoadDrafts : ExtensionStoreAction
    data class OpenDraft(val item: ExtensionStoreItem) : ExtensionStoreAction
    data object ClearEditingDraft : ExtensionStoreAction
    data class DeleteDraft(val item: ExtensionStoreItem) : ExtensionStoreAction
}
