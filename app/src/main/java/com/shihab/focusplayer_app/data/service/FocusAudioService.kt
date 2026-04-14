package com.shihab.focusplayer_app.data.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures

class FocusAudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val players = mutableMapOf<Int, ExoPlayer>()

    companion object {
        const val CMD_PLAY_AUDIO = "CMD_PLAY_AUDIO"
        const val CMD_STOP_AUDIO = "CMD_STOP_AUDIO"
        const val CMD_UPDATE_VOLUME = "CMD_UPDATE_VOLUME"
        const val CMD_STOP_ALL = "CMD_STOP_ALL"
    }

    override fun onCreate() {
        super.onCreate()
        val defaultPlayer = createPlayer()
        
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, defaultPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(FocusSessionCallback())
            .build()
    }

    private fun createPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }

    private inner class FocusSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_PLAY_AUDIO -> {
                    val id = args.getInt("id")
                    val resId = args.getInt("resId")
                    val volume = args.getFloat("volume", 0.5f)
                    playAudio(id, resId, volume)
                }
                CMD_STOP_AUDIO -> {
                    val id = args.getInt("id")
                    stopAudio(id)
                }
                CMD_UPDATE_VOLUME -> {
                    val id = args.getInt("id")
                    val volume = args.getFloat("volume", 0.5f)
                    updateVolume(id, volume)
                }
                CMD_STOP_ALL -> {
                    stopAll()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun playAudio(id: Int, resId: Int, volume: Float) {
        val player = players.getOrPut(id) { createPlayer() }
        val uri = Uri.parse("android.resource://$packageName/$resId")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.volume = volume
        player.prepare()
        player.play()
    }

    private fun stopAudio(id: Int) {
        players[id]?.let {
            it.stop()
            it.release()
            players.remove(id)
        }
    }

    private fun updateVolume(id: Int, volume: Float) {
        players[id]?.volume = volume
    }

    private fun stopAll() {
        players.values.forEach { 
            it.stop()
            it.release()
        }
        players.clear()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (players.isEmpty() || players.values.none { it.isPlaying }) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopAll()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
