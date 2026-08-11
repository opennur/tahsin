package com.tahsin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tahsin.app.theme.AyahTheme
import com.tahsin.app.ui.AudioManagerScreen
import com.tahsin.app.ui.TahsinScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AyahTheme {
                var showAudioManager by remember { mutableStateOf(false) }
                if (showAudioManager) {
                    AudioManagerScreen(onBack = { showAudioManager = false })
                } else {
                    TahsinScreen(onOpenAudioManager = { showAudioManager = true })
                }
            }
        }
    }
}
