package dev.fishpi.mobile.feature.extensionstore

internal sealed interface ExtensionStoreEffect {
    data class ShowMessage(val message: String) : ExtensionStoreEffect
    data class ShowError(val message: String) : ExtensionStoreEffect
    data object UploadFinished : ExtensionStoreEffect
}
