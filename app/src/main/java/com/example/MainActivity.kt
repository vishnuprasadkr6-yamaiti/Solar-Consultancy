package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.SolarDeskApp
import com.example.ui.theme.SolarDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SolarDeskApplication
        setContent {
            SolarDeskTheme {
                SolarDeskApp(repository = app.repository)
            }
        }
    }
}
