package dev.fishpi.mobile.ui.overlay

import dev.fishpi.mobile.*
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.fishpi.mobile.shared.message.native.openSystemVideo
import kotlinx.coroutines.delay

@Composable
internal fun VideoPlaybackOverlay(
    url: String,
    session: PlaybackSession? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val ownedSession = remember(url, session) {
        if (session == null) PlaybackSession(context) else null
    }
    LaunchedEffect(ownedSession, url) {
        ownedSession?.prepare(url)
    }
    val activeSession = session ?: ownedSession ?: return
    val player = activeSession.player

    var controlsVisible by remember(activeSession) { mutableStateOf(true) }
    var isPlaying by remember(activeSession) { mutableStateOf(false) }
    var hasError by remember(activeSession) { mutableStateOf(false) }
    var positionMs by remember(activeSession) { mutableLongStateOf(0L) }
    var durationMs by remember(activeSession) { mutableLongStateOf(0L) }
    var isScrubbing by remember(activeSession) { mutableStateOf(false) }
    var scrubPositionMs by remember(activeSession) { mutableLongStateOf(0L) }

    DisposableEffect(activeSession) {
        activeSession.enterFullscreen()
        onDispose {
            activeSession.exitFullscreen()
            ownedSession?.release()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = player.duration
                durationMs = if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration
                durationMs = if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
            }
            delay(250)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isScrubbing) {
        if (controlsVisible && isPlaying && !isScrubbing) {
            delay(2200)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                controlsVisible = !controlsVisible
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.keepScreenOn = player.isPlaying
            },
            onRelease = { view ->
                view.player = null
            },
        )

        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "无法播放",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                )
                TextButton(
                    onClick = {
                        hasError = false
                        activeSession.retry()
                    },
                ) {
                    Text(text = "重试", color = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.38f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭")
                    }
                    FilledIconButton(
                        onClick = { context.openSystemVideo(url) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.38f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "系统播放器")
                    }
                }

                if (!hasError) {
                    FilledIconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.38f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Slider(
                        value = if (isScrubbing) scrubPositionMs.toFloat() else positionMs.toFloat(),
                        onValueChange = {
                            isScrubbing = true
                            scrubPositionMs = it.toLong()
                        },
                        onValueChangeFinished = {
                            player.seekTo(scrubPositionMs)
                            isScrubbing = false
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatVideoTime(if (isScrubbing) scrubPositionMs else positionMs),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("/", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(formatVideoTime(durationMs), color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
