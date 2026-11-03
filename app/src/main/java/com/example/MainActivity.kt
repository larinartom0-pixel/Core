package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.MainMenuScreen
import com.example.ui.theme.IndustrialDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IndustrialDark
                ) {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val showFlowArrows by viewModel.showFlowArrows.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    when (currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                onStartGame = { viewModel.startNewGame() },
                showGrid = showGrid,
                onToggleGrid = { viewModel.toggleGrid(it) },
                showFlowArrows = showFlowArrows,
                onToggleFlowArrows = { viewModel.toggleFlowArrows(it) },
                soundEnabled = soundEnabled,
                onToggleSound = { viewModel.toggleSound(it) }
            )
        }

        AppScreen.IN_GAME -> {
            GameScreen(
                viewModel = viewModel,
                onExitToMainMenu = { viewModel.exitToMainMenu() }
            )
        }
    }
}
