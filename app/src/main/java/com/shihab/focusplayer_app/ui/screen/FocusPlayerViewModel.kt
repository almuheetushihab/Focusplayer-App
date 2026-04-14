package com.shihab.focusplayer_app.ui.screen

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shihab.focusplayer_app.data.service.FocusAudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.shihab.focusplayer_app.R
import com.shihab.focusplayer_app.domain.model.AudioModel
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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioList = MutableStateFlow<List<AudioModel>>(emptyList())
    val audioList: StateFlow<List<AudioModel>> = _audioList.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioModel?>(null)
    val currentAudio: StateFlow<AudioModel?> = _currentAudio.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Sleep Timer States
    private val _remainingTime = MutableStateFlow<Long?>(null) // in milliseconds
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        _audioList.value = listOf(
            AudioModel(1, "Typing Focus", "Deep mechanical typing sound", R.raw.asmr_typing),
            AudioModel(2, "Soft Rain", "Gentle rain on the window", R.raw.asmr_typing),
            AudioModel(3, "Deep Forest", "Bird chirping and wind", R.raw.asmr_typing),
            AudioModel(4, "Cafe Ambience", "Light chatter and coffee cups", R.raw.asmr_typing)
        )
    }

    fun initializeController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, FocusAudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controller ?: return@addListener
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressUpdate()
                    } else {
                        stopProgressUpdate()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val currentId = mediaItem?.mediaId?.toIntOrNull()
                    _currentAudio.value = _audioList.value.find { it.id == currentId }
                }
            })
            _isPlaying.value = controller.isPlaying
            _duration.value = controller.duration
            if (controller.isPlaying) startProgressUpdate()
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = controller?.currentPosition ?: 0L
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun playAudio(audio: AudioModel, context: Context) {
        val controller = controller ?: return
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(audio.id.toString())
            .setUri("android.resource://${context.packageName}/${audio.audioResId}")
            .build()
            
        controller.setMediaItem(mediaItem)
        controller.repeatMode = Player.REPEAT_MODE_ALL
        controller.prepare()
        controller.play()
        _currentAudio.value = audio
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun stopAudio() {
        controller?.stop()
        cancelSleepTimer()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun setVolume(volume: Float) {
        _volume.value = volume
        controller?.volume = volume
    }

    // Sleep Timer Logic
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _remainingTime.value = null
            controller?.volume = _volume.value // Restore original volume
            return
        }
        
        val millis = minutes * 60 * 1000L
        _remainingTime.value = millis
        
        val fadeOutDuration = 30 * 1000L // শেষ ৩০ সেকেন্ডে ফেড-আউট শুরু হবে
        
        sleepTimerJob = viewModelScope.launch {
            var currentMillis = millis
            while (currentMillis > 0) {
                delay(1000)
                currentMillis -= 1000
                _remainingTime.value = currentMillis
                
                // ফেড-আউট লজিক: শেষ ৩০ সেকেন্ডে ভলিউম ধীরে ধীরে কমানো
                if (currentMillis <= fadeOutDuration) {
                    val fadePercentage = currentMillis.toFloat() / fadeOutDuration.toFloat()
                    controller?.volume = _volume.value * fadePercentage
                }
            }
            // টাইমার শেষ হলে অডিও বন্ধ করে দাও
            stopAudio()
            _remainingTime.value = null
            // পরের বার চালানোর জন্য ভলিউম আগের জায়গায় ফিরিয়ে আনা
            controller?.volume = _volume.value
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _remainingTime.value = null
        controller?.volume = _volume.value // টাইমার ক্যানসেল করলে ভলিউম আগের মতো করে দাও
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
