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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min

// Game constants
const val BOARD_WIDTH = 10
const val BOARD_HEIGHT = 20

// Tetromino shapes
val shapes = listOf(
    listOf(listOf(1, 1, 1, 1)), // I
    listOf(listOf(1, 1), listOf(1, 1)), // O
    listOf(listOf(0, 1, 0), listOf(1, 1, 1)), // T
    listOf(listOf(0, 1, 1), listOf(1, 1, 0)), // S
    listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // Z
    listOf(listOf(1, 0, 0), listOf(1, 1, 1)), // J
    listOf(listOf(0, 0, 1), listOf(1, 1, 1))  // L
)

val colors = listOf(
    Color.Cyan,
    Color.Yellow,
    Color.Magenta,
    Color.Green,
    Color.Red,
    Color.Blue,
    Color.White
)

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

@Composable
fun TetrisGame(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        GameBoard(gameState.board, gameState.currentPiece, Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    Button(onClick = { gameViewModel.movePiece(-1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                    }
                    Button(onClick = { gameViewModel.rotatePiece() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rotate")
                    }
                    Button(onClick = { gameViewModel.movePiece(1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                    }
                }
                Button(onClick = { gameViewModel.movePiece(0, 1) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down")
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