package com.example.petling.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.petling.PetlingApplication
import com.example.petling.di.AppContainer

/** Composable에서 AppContainer 접근. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as PetlingApplication).container
