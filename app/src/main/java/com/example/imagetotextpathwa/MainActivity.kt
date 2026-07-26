package com.example.imagetotextpathwa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.imagetotextpathwa.ui.theme.ImageToTextPathwaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageToTextPathwaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

                    when (currentDestination) {
                        AppDestination.HOME -> {
                            MainHomeScreen(onNavigate = { destination ->
                                currentDestination = destination
                            })
                        }
                        AppDestination.ML_KIT -> {
                            remember { MLKitDemonstration() }.DemonstrationMenu(onExit = {
                                currentDestination = AppDestination.HOME
                            })
                        }
                        AppDestination.TF_LITE -> {
                            TFLiteDemonstration(onBack = {
                                currentDestination = AppDestination.HOME
                            })
                        }
                    }
                }
            }
        }
    }
}
