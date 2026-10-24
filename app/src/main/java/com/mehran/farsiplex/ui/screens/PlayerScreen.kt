package com.mehran.farsiplex.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val OVERLAY_VISIBLE_MS = 4500L

/**
 * Full-screen ExoPlayer using the signed CDN URL resolved by the repository.
 * Holds FLAG_KEEP_SCREEN_ON while playing so a TV box's screensaver or idle
 * timer never interrupts a film.
 */
@Composable
fun PlayerScreen(url: String, title: String, subtitle: String?) {
    val context = LocalContext.current
    var overlayVisible by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(true) }

    val player = remember(url) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                keepScreenOn = player.wantsScreenOn()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                keepScreenOn = player.wantsScreenOn()
                buffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "ارتباط با سرور پخش قطع شد"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "فایل ویدیو در دسترس نیست (خطای سرور)"
                    else -> "پخش این ویدیو ممکن نشد"
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val activity = context.findActivity()
    DisposableEffect(activity, keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(Unit) {
        delay(OVERLAY_VISIBLE_MS)
        overlayVisible = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                        )
                    )
            ) {
                Column(Modifier.align(Alignment.TopStart).padding(32.dp)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFFB9C2D6), fontSize = 14.sp)
                    }
                }
            }
        }

        errorMessage?.let { message ->
            Box(
                Modifier.fillMaxSize().background(Color(0xE6000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, color = Color(0xFFFFB4AB), fontSize = 20.sp)
                    Text(
                        "برای بازگشت دکمه Back را بزنید",
                        color = Color(0xFF98A4BC),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

/** Screen-on only while playback is actually wanted (not paused or ended). */
private fun Player.wantsScreenOn(): Boolean =
    playWhenReady &&
        playbackState != Player.STATE_IDLE &&
        playbackState != Player.STATE_ENDED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
