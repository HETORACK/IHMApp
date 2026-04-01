package com.example.ihm

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
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

            // SOLICITUD DE PERMISO DE NOTIFICACIONES (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    // Aquí podrías manejar si el usuario deniega el permiso
                }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (context as Activity).window
                    val colorEspecifico = Color(0xFF000000).toArgb()
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
