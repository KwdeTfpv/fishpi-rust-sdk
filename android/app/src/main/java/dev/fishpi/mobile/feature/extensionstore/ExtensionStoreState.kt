package dev.fishpi.mobile.feature.extensionstore

import dev.fishpi.mobile.data.ExtensionStoreItem
import dev.fishpi.mobile.data.ExtensionStoreSession
import dev.fishpi.mobile.ui.state.ListLoadState

internal data class ExtensionStoreState(
    val session: ExtensionStoreSession? = null,
    val authError: String? = null,
    val isAuthenticating: Boolean = false,
    val selectedFilter: StoreFilter = StoreFilter.All,
    val query: String = "",
    val items: List<ExtensionStoreItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val list: ListLoadState = ListLoadState(isEmpty = true),
    val purchasedItems: List<ExtensionStoreItem> = emptyList(),
    val purchasingId: Long? = null,
    val isUploading: Boolean = false,
    val uploadSuccessCount: Int = 0,
    val drafts: List<ExtensionStoreItem> = emptyList(),
    val draftsPage: Int = 1,
    val draftsList: ListLoadState = ListLoadState(isEmpty = true),
    val openingDraftId: Long? = null,
    val editingDraft: ExtensionStoreItem? = null,
    val deletingDraftId: Long? = null,
)
