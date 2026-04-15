package com.shihab.focusplayer_app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shihab.focusplayer_app.domain.model.AudioModel
import com.shihab.focusplayer_app.domain.model.AudioState
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun FocusPlayerScreen(
    viewModel: FocusPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val audioList by viewModel.audioList.collectAsState()
    val audioStates by viewModel.audioStates.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Focus Player",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mix your favorite sounds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            IconButton(onClick = { showTimerDialog = true }) {
                Icon(
                    imageVector = if (remainingTime != null) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Sleep Timer",
                    tint = if (remainingTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        if (remainingTime != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sleep Timer: ${formatTime(remainingTime!!)} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Sound List with Mixing Controls
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(audioList) { audio ->
                val state = audioStates[audio.id] ?: AudioState(audio.id)
                AudioMixItem(
                    audio = audio,
                    state = state,
                    onToggle = { viewModel.toggleAudio(audio) },
                    onVolumeChange = { viewModel.updateAudioVolume(audio.id, it) }
                )
            }
        }

        // Global Stop Button
        if (audioStates.values.any { it.isPlaying }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.stopAll() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop All",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showTimerDialog = false },
            onSelectTime = { minutes ->
                viewModel.setSleepTimer(minutes)
                showTimerDialog = false
            }
        )
    }
}

@Composable
fun AudioMixItem(
    audio: AudioModel,
    state: AudioState,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val containerColor by animateColorAsState(
        if (state.isPlaying) MaterialTheme.colorScheme.primaryContainer 
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (state.isPlaying) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (state.isPlaying) Color.White else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = audio.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = audio.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.background(
                        if (state.isPlaying) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (state.isPlaying) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Slider(
                        value = state.volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelectTime: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Sleep Timer") },
        text = {
            Column {
                val times = listOf(0, 5, 15, 30, 60)
                times.forEach { mins ->
                    TextButton(
                        onClick = { onSelectTime(mins) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (mins == 0) "Off" else "$mins Minutes")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
