package com.shihab.focusplayer_app.domain.model

data class AudioState(
    val audioId: Int,
    val isPlaying: Boolean = false,
    val volume: Float = 0.5f
)
