package dev.fishpi.mobile.ui.blur

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

internal val LocalFishPiBlurTarget = staticCompositionLocalOf<BlurTarget?> { null }

@Composable
internal fun ProvideFishPiBlurTarget(
    target: BlurTarget?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFishPiBlurTarget provides target, content = content)
}

@Composable
internal fun FishPiBlurBackdrop(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.20f),
    blurRadius: Float = 22f,
) {
    val target = LocalFishPiBlurTarget.current
    if (target == null) {
        Box(modifier = modifier.background(overlayColor))
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            BlurView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setupWith(target)
                    .setFrameClearDrawable(target.rootView.background)
                    .setBlurRadius(blurRadius)
                setOverlayColor(overlayColor.toArgb())
            }
        },
        update = { view ->
            view.setupWith(target)
                .setFrameClearDrawable(target.rootView.background)
                .setBlurRadius(blurRadius)
            view.setOverlayColor(overlayColor.toArgb())
        },
    )
}

@Composable
internal fun FishPiBlurOverlay(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.20f),
    blurRadius: Float = 22f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        FishPiBlurBackdrop(
            modifier = Modifier.fillMaxSize(),
            overlayColor = overlayColor,
            blurRadius = blurRadius,
        )
        content()
    }
}
