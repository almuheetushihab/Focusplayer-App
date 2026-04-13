package com.shihab.focusplayer_app.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusPlayerScreen(viewModel: FocusPlayerViewModel) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Deep Focus Mode", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                if (isPlaying) viewModel.pauseAudio(context) else viewModel.playAudio(context)
            }) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            Button(onClick = { viewModel.stopAudio(context) }) {
                Text("Stop")
            }
        }
    }
}