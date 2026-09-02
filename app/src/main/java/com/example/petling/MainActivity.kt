package com.example.petling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.petling.ui.navigation.PetlingNavHost
import com.example.petling.ui.theme.PetlingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetlingTheme {
                PetlingNavHost()
            }
        }
    }
}
