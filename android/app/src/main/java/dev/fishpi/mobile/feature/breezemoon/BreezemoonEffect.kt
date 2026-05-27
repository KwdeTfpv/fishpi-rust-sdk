package dev.fishpi.mobile.feature.breezemoon

internal sealed interface BreezemoonEffect {
    data class ShowMessage(val message: String) : BreezemoonEffect
    data class ShowError(val message: String) : BreezemoonEffect
    data object OpenGalleryPicker : BreezemoonEffect
    data object OpenCameraPicker : BreezemoonEffect
}
