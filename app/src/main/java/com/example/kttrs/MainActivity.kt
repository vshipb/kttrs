package com.example.kttrs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kttrs.ui.ControlMode
import com.example.kttrs.ui.SettingsScreen
import kotlin.math.abs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import com.example.kttrs.GameConstants.BOARD_HEIGHT
import com.example.kttrs.GameConstants.BOARD_WIDTH
import com.example.kttrs.GameConstants.colors
import kotlin.math.min

data class Piece(
    val shape: List<List<Int>>,
    val color: Color,
    val x: Int,
    val y: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TetrisGame()
                }
            }
        }
    }
}

class GameViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun TetrisGame(gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory(LocalContext.current.applicationContext as Application))) {
    val gameState by gameViewModel.gameState.collectAsState()
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

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(gameState.controlMode) {
            if (gameState.controlMode != ControlMode.Buttons) {
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
                                gameViewModel.movePiece(1, 0)
                            } else {
                                gameViewModel.movePiece(-1, 0)
                            }
                        } else {
                            if (dragDistanceY > 0) {
                                gameViewModel.movePiece(0, 1)
                            } else {
                                gameViewModel.rotatePiece()
                            }
                        }
                    }
                )
            }
        }) {
        GameBoard(gameState.board, gameState.currentPiece, Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            if (gameState.controlMode != ControlMode.Swipes) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (gameState.gameOver) {
                        Text("Game Over", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    }
                    Text("Score: ${gameState.score}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = { gameViewModel.movePiece(-1, 0) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                        }
                        OutlinedButton(onClick = { gameViewModel.rotatePiece() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rotate")
                        }
                        OutlinedButton(onClick = { gameViewModel.movePiece(1, 0) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                        }
                    }
                    OutlinedButton(onClick = { gameViewModel.movePiece(0, 1) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down")
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(board: Array<IntArray>, piece: Piece, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.Black)) {
        val cellSize = min(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT)
        // Draw board
        for (y in board.indices) {
            for (x in board[y].indices) {
                if (board[y][x] != 0) {
                    drawRect(
                        color = colors[board[y][x] - 1],
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
        // Draw piece
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] == 1) {
                    drawRect(
                        color = piece.color,
                        topLeft = Offset((piece.x + x) * cellSize, (piece.y + y) * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}