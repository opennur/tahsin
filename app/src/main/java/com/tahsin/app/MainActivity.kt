package com.tahsin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tahsin.app.theme.AyahTheme
import com.tahsin.app.ui.TahsinScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AyahTheme {
                TahsinScreen()
            }
        }
    }
}
