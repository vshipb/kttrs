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
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import com.example.kttrs.data.SettingsDataStore
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

class GameViewModelFactory(private val settingsDataStore: SettingsDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun TetrisGame(gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory(SettingsDataStore(LocalContext.current)))) {
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
                            if (dragDistanceY > 200) { // Hard drop threshold
                                gameViewModel.hardDrop()
                            } else if (dragDistanceY > 0) {
                                gameViewModel.movePiece(0, 1)
                            } else {
                                gameViewModel.rotatePieceRight()
                            }
                        }
                    }
                )
            }
        }) {
        GameBoard(gameState.board, gameState.currentPiece, Modifier.fillMaxSize())

        if(gameState.gameOver) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { gameViewModel.restartGame() }) {
                        Text("Restart")
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                    Text("Score: ${gameState.score}", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("Lines: ${gameState.linesCleared}", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Next", color = Color.White)
                    PiecePreview(piece = gameState.nextPiece, modifier = Modifier.size(80.dp))
                    Text("Hold", color = Color.White)
                    PiecePreview(piece = gameState.heldPiece, modifier = Modifier.size(80.dp))
                }
            }

            if (gameState.controlMode != ControlMode.Swipes) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            OutlinedButton(onClick = { gameViewModel.rotatePieceLeft() }) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { gameViewModel.rotatePieceRight() }) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(onClick = { gameViewModel.movePiece(-1, 0) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { gameViewModel.movePiece(1, 0) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { gameViewModel.hardDrop() }) {
                            Icon(Icons.Filled.KeyboardDoubleArrowDown, contentDescription = "Hard Drop")
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedButton(onClick = { gameViewModel.holdPiece() }) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Hold")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { gameViewModel.movePiece(0, 1) }) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Soft Drop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PiecePreview(piece: Piece?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (piece != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = min(size.width / 4, size.height / 4)
                for (y in piece.shape.indices) {
                    for (x in piece.shape[y].indices) {
                        if (piece.shape[y][x] == 1) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(piece.color, piece.color.copy(alpha = 0.5f))
                                ),
                                topLeft = Offset(x * cellSize, y * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.2f),
                                topLeft = Offset(x * cellSize, y * cellSize),
                                size = Size(cellSize, cellSize),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(board: Array<IntArray>, piece: Piece, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Brush.verticalGradient(listOf(Color.DarkGray, Color.Black)))) {
        val cellSize = min(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT)
        // Draw board
        for (y in board.indices) {
            for (x in board[y].indices) {
                if (board[y][x] != 0) {
                    val color = colors[board[y][x] - 1]
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(color, color.copy(alpha = 0.5f))
                        ),
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
            }
        }
        // Draw piece
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] == 1) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(piece.color, piece.color.copy(alpha = 0.5f))
                        ),
                        topLeft = Offset((piece.x + x) * cellSize, (piece.y + y) * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset((piece.x + x) * cellSize, (piece.y + y) * cellSize),
                        size = Size(cellSize, cellSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
            }
        }
    }
}