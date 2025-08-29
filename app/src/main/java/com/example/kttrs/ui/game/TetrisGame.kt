package com.example.kttrs.ui.game

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kttrs.GameViewModel
import com.example.kttrs.GameViewModelFactory
import com.example.kttrs.data.SettingsDataStore
import com.example.kttrs.ui.ControlMode
import com.example.kttrs.ui.SettingsScreen
import kotlin.math.abs

@Composable
fun TetrisGame(gameViewModel: GameViewModel = viewModel(
    factory = GameViewModelFactory(
        SettingsDataStore(LocalContext.current)
    )
)
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val showGhostPiece by gameViewModel.showGhostPiece.collectAsState(initial = true)
    val topScore by gameViewModel.topScore.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                SettingsScreen(
                    currentControlMode = gameState.controlMode,
                    onControlModeChange = {
                        gameViewModel.setControlMode(it)
                    },
                    showGhostPiece = showGhostPiece,
                    onShowGhostPieceChange = {
                        gameViewModel.saveShowGhostPiece(it)
                    }
                )
            },
            confirmButton = {
                OutlinedButton(onClick = { showSettings = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .pointerInput(gameState.controlMode) {
                if (gameState.controlMode == ControlMode.Swipes || gameState.controlMode == ControlMode.Both) {
                    var dragDistanceX = 0f
                    var dragDistanceY = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDistanceX += dragAmount.x
                            dragDistanceY += dragAmount.y
                        },
                        onDragEnd = {
                            if (abs(dragDistanceX) > abs(dragDistanceY)) {
                                if (dragDistanceX > 0) {
                                    gameViewModel.movePiece(1)
                                } else {
                                    gameViewModel.movePiece(-1)
                                }
                            } else {
                                if (dragDistanceY > 200) { // Hard drop threshold
                                    gameViewModel.hardDrop()
                                } else if (dragDistanceY > 0) {
                                    gameViewModel.softDrop()
                                } else {
                                    gameViewModel.rotatePieceRight()
                                }
                            }
                        }
                    )
                }
            }) {
        GameBoard(
            board = gameState.board,
            currentPiece = gameState.currentPiece,
            ghostPiece = gameState.ghostPiece,
            showGhostPiece = showGhostPiece,
            clearingLines = gameState.clearingLines, // Add this
            modifier = Modifier.Companion.fillMaxSize()
        )

        if (gameState.gameOver) {
            Box(
                modifier = Modifier.Companion.fillMaxSize(),
                contentAlignment = Alignment.Companion.Center
            ) {
                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    Text(
                        "Game Over",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Companion.Yellow
                    )
                    Spacer(modifier = Modifier.Companion.height(16.dp))
                    Button(onClick = { gameViewModel.restartGame() }) {
                        Text("Restart",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.Yellow)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.Companion.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.Top
            ) {
                Column(horizontalAlignment = Alignment.Companion.Start) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = Color.Companion.White
                        )
                    }
                    Text(
                        "Top: $topScore",
                        color = Color.Companion.Yellow,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.Companion.height(12.dp))
                    Text(
                        "Score: ${gameState.score}",
                        color = Color.Companion.Green,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.Companion.height(12.dp))
                    Text(
                        "Lines: ${gameState.linesCleared}",
                        color = Color.Companion.Blue,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Column(horizontalAlignment = Alignment.Companion.End) {
                    Text("Next", color = Color.Companion.White)
                    PiecePreview(
                        piece = gameState.nextPiece,
                        modifier = Modifier.Companion.size(80.dp)
                    )
                    Text("Hold", color = Color.Companion.White)
                    PiecePreview(
                        piece = gameState.heldPiece,
                        modifier = Modifier.Companion.size(80.dp)
                    )
                }
            }

            if (gameState.controlMode == ControlMode.Buttons || gameState.controlMode == ControlMode.Both) {
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Companion.Bottom
                ) {
                    Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                        Row {
                            OutlinedButton(onClick = { gameViewModel.rotatePieceLeft() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "Rotate Left",
                                    modifier = Modifier.Companion.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.Companion.width(8.dp))
                            OutlinedButton(onClick = { gameViewModel.rotatePieceRight() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "Rotate Right",
                                    modifier = Modifier.Companion.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.Companion.height(8.dp))
                        Row {
                            OutlinedButton(onClick = { gameViewModel.movePiece(-1) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Left",
                                    modifier = Modifier.Companion.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.Companion.width(8.dp))
                            OutlinedButton(onClick = { gameViewModel.movePiece(1) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Right",
                                    modifier = Modifier.Companion.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.Companion.height(8.dp))
                        OutlinedButton(onClick = { gameViewModel.hardDrop() }) {
                            Icon(
                                Icons.Filled.KeyboardDoubleArrowDown,
                                contentDescription = "Hard Drop",
                                modifier = Modifier.Companion.size(48.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                        OutlinedButton(onClick = { gameViewModel.holdPiece() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Hold",
                                modifier = Modifier.Companion.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.Companion.height(8.dp))
                        OutlinedButton(onClick = { gameViewModel.softDrop() }) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Soft Drop",
                                modifier = Modifier.Companion.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}