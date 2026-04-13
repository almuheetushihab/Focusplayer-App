package com.shihab.focusplayer_app.ui.screen


import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.shihab.focusplayer_app.data.service.FocusAudioService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FocusPlayerViewModel : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun playAudio(context: Context) {
        sendCommandToService(context, "ACTION_PLAY")
        _isPlaying.value = true
    }

    fun pauseAudio(context: Context) {
        sendCommandToService(context, "ACTION_PAUSE")
        _isPlaying.value = false
    }

    fun stopAudio(context: Context) {
        sendCommandToService(context, "ACTION_STOP")
        _isPlaying.value = false
    }

    private fun sendCommandToService(context: Context, action: String) {
        val intent = Intent(context, FocusAudioService::class.java).apply {
            this.action = action
        }

        ContextCompat.startForegroundService(context, intent)
    }
}