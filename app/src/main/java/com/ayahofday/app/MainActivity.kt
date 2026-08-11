package com.ayahofday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ayahofday.app.theme.AyahTheme
import com.ayahofday.app.ui.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AyahTheme {
                AppNavGraph()
            }
        }
    }
}
