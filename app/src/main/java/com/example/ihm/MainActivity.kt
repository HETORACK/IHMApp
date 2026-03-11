package com.example.ihm

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.example.ihm.ui.MainScreen
import com.example.ihm.ui.theme.IHMTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val view = LocalView.current
            val context = LocalContext.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (context as Activity).window

                    // 1. Define tu color específico (ejemplo: el púrpura de tu barra)
                    val colorEspecifico = Color(0xFF000000).toArgb()

                    // 2. Aplica el color a la barra de navegación
                    window.navigationBarColor = colorEspecifico
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightNavigationBars = false
                        isAppearanceLightStatusBars = false
                    }
                }
            }
            IHMTheme {
                MainScreen()
            }
        }
    }
}
