package dev.fishpi.mobile

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder

@Composable
internal fun rememberFishPiImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        FishPiImageLoaderHolder.get(context.applicationContext)
    }
}

private object FishPiImageLoaderHolder {
    @Volatile
    private var shared: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return shared ?: synchronized(this) {
            shared ?: ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(SvgDecoder.Factory())
                }
                .build()
                .also { shared = it }
        }
    }
}
