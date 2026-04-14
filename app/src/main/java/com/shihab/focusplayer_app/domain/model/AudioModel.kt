package com.shihab.focusplayer_app.domain.model

data class AudioModel(
    val id: Int,
    val title: String,
    val description: String,
    val audioResId: Int,
    val iconResId: Int? = null
)
