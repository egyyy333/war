package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.trenchwar.game.GameViewModel
import com.example.trenchwar.game.GameViewModel.GameState
import com.example.trenchwar.ui.GameScreen
import com.example.trenchwar.ui.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val gameState by viewModel.gameState.collectAsState()
                val highestStage by viewModel.highestStage.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .navigationBarsPadding()
                ) {
                    Crossfade(
                        targetState = gameState,
                        label = "screen_transition"
                    ) { state ->
                        when (state) {
                            GameState.MENU, GameState.STAGE_SELECT -> {
                                MainMenuScreen(
                                    highestStage = highestStage,
                                    onStageSelected = { stage ->
                                        viewModel.selectStage(stage)
                                    }
                                )
                            }
                            GameState.PLAYING, GameState.PAUSED, GameState.VICTORY, GameState.DEFEAT -> {
                                GameScreen(
                                    viewModel = viewModel,
                                    onBackToMenu = {
                                        viewModel.setGameState(GameState.MENU)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
