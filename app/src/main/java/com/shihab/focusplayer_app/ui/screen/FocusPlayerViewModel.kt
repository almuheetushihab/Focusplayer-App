package com.shihab.focusplayer_app.ui.screen

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.shihab.focusplayer_app.R
import com.shihab.focusplayer_app.data.service.FocusAudioService
import com.shihab.focusplayer_app.domain.model.AudioModel
import com.shihab.focusplayer_app.domain.model.AudioState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FocusPlayerViewModel : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _audioList = MutableStateFlow<List<AudioModel>>(emptyList())
    val audioList: StateFlow<List<AudioModel>> = _audioList.asStateFlow()

    // প্রতিটি অডিওর স্টেট (playing, volume) ট্রাক করার জন্য
    private val _audioStates = MutableStateFlow<Map<Int, AudioState>>(emptyMap())
    val audioStates: StateFlow<Map<Int, AudioState>> = _audioStates.asStateFlow()

    // Sleep Timer States
    private val _remainingTime = MutableStateFlow<Long?>(null)
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        _audioList.value = listOf(
            AudioModel(1, "Typing Focus", "Deep mechanical typing sound", R.raw.asmr_typing),
            AudioModel(2, "Soft Rain", "Gentle rain on the window", R.raw.asmr_typing),
            AudioModel(3, "Deep Forest", "Bird chirping and wind", R.raw.asmr_typing),
            AudioModel(4, "Cafe Ambience", "Light chatter and coffee cups", R.raw.asmr_typing)
        )
        // ইনিশিয়াল স্টেট তৈরি করা
        _audioStates.value = _audioList.value.associate { it.id to AudioState(it.id) }
    }

    fun initializeController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, FocusAudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({}, MoreExecutors.directExecutor())
    }

    fun toggleAudio(audio: AudioModel) {
        val currentState = _audioStates.value[audio.id] ?: return
        val newState = currentState.copy(isPlaying = !currentState.isPlaying)
        
        _audioStates.value = _audioStates.value + (audio.id to newState)
        
        if (newState.isPlaying) {
            sendCustomCommand(FocusAudioService.CMD_PLAY_AUDIO, Bundle().apply {
                putInt("id", audio.id)
                putInt("resId", audio.audioResId)
                putFloat("volume", newState.volume)
            })
        } else {
            sendCustomCommand(FocusAudioService.CMD_STOP_AUDIO, Bundle().apply {
                putInt("id", audio.id)
            })
        }
    }

    fun updateAudioVolume(audioId: Int, volume: Float) {
        val currentState = _audioStates.value[audioId] ?: return
        val newState = currentState.copy(volume = volume)
        _audioStates.value = _audioStates.value + (audioId to newState)
        
        if (newState.isPlaying) {
            sendCustomCommand(FocusAudioService.CMD_UPDATE_VOLUME, Bundle().apply {
                putInt("id", audioId)
                putFloat("volume", volume)
            })
        }
    }

    private fun sendCustomCommand(action: String, args: Bundle = Bundle.EMPTY) {
        controller?.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
    }

    fun stopAll() {
        sendCustomCommand(FocusAudioService.CMD_STOP_ALL)
        _audioStates.value = _audioStates.value.mapValues { it.value.copy(isPlaying = false) }
        cancelSleepTimer()
    }

    // Sleep Timer with Multi-audio Fade-out
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _remainingTime.value = null
            restoreAllVolumes()
            return
        }
        
        val millis = minutes * 60 * 1000L
        _remainingTime.value = millis
        val fadeOutDuration = 30 * 1000L
        
        sleepTimerJob = viewModelScope.launch {
            var currentMillis = millis
            while (currentMillis > 0) {
                delay(1000)
                currentMillis -= 1000
                _remainingTime.value = currentMillis
                
                if (currentMillis <= fadeOutDuration) {
                    val fadePercentage = currentMillis.toFloat() / fadeOutDuration.toFloat()
                    fadeAllVolumes(fadePercentage)
                }
            }
            stopAll()
            _remainingTime.value = null
            restoreAllVolumes()
        }
    }

    private fun fadeAllVolumes(percentage: Float) {
        _audioStates.value.forEach { (id, state) ->
            if (state.isPlaying) {
                val fadedVolume = state.volume * percentage
                sendCustomCommand(FocusAudioService.CMD_UPDATE_VOLUME, Bundle().apply {
                    putInt("id", id)
                    putFloat("volume", fadedVolume)
                })
            }
        }
    }

    private fun restoreAllVolumes() {
        _audioStates.value.forEach { (id, state) ->
            if (state.isPlaying) {
                sendCustomCommand(FocusAudioService.CMD_UPDATE_VOLUME, Bundle().apply {
                    putInt("id", id)
                    putFloat("volume", state.volume)
                })
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _remainingTime.value = null
        restoreAllVolumes()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
