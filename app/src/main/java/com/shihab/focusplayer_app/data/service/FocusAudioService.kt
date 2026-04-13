package com.shihab.focusplayer_app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shihab.focusplayer_app.R


class FocusAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "FocusAudioChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.asmr_typing)
        mediaPlayer?.isLooping = true
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val action = intent?.action

        when (action) {
            "ACTION_PLAY" -> {
                mediaPlayer?.start()
                startForeground(1, createNotification("Playing ASMR Focus Audio..."))
            }

            "ACTION_PAUSE" -> {
                mediaPlayer?.pause()
                startForeground(1, createNotification("Audio Paused"))
            }

            "ACTION_STOP" -> {
                mediaPlayer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Player")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Focus Audio Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}