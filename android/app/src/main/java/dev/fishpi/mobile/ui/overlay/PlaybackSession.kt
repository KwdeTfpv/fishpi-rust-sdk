package dev.fishpi.mobile.ui.overlay

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

internal class PlaybackSession(context: Context) {
    private val appContext = context.applicationContext
    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
    }

    private var inlineView: PlayerView? = null
    private var fullscreen = false
    private var currentUrl: String? = null
    private var released = false
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            player.pause()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                inlineView?.keepScreenOn = isPlaying
            }
        })
    }

    fun setSource(url: String) {
        if (released || currentUrl == url) return
        currentUrl = url
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
    }

    fun prepare(url: String, playWhenReady: Boolean = true) {
        if (released) return
        setSource(url)
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
        }
        player.playWhenReady = playWhenReady
    }

    fun bindInline(view: PlayerView) {
        if (released) return
        inlineView = view
        if (!fullscreen) view.player = player
    }

    fun unbindInline(view: PlayerView) {
        if (inlineView !== view) return
        view.player = null
        inlineView = null
        releaseIfIdle()
    }

    fun enterFullscreen() {
        fullscreen = true
        inlineView?.player = null
    }

    fun exitFullscreen() {
        fullscreen = false
        val view = inlineView
        if (view != null && view.isAttachedToWindow) {
            view.player = player
        } else {
            inlineView = null
        }
        releaseIfIdle()
    }

    fun retry() {
        if (released) return
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        if (released) return
        released = true
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        inlineView?.player = null
        inlineView = null
        player.release()
    }

    internal fun releaseIfIdle() {
        if (!fullscreen && inlineView == null) release()
    }
}
