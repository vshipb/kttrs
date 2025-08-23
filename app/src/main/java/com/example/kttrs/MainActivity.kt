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
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

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
    var x: Int,
    var y: Int
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
fun TetrisGame() {
    var board by remember { mutableStateOf(Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }) }
    var currentPiece by remember { mutableStateOf(randomPiece()) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    fun isValidPosition(piece: Piece): Boolean {
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] == 1) {
                    val newX = piece.x + x
                    val newY = piece.y + y
                    if (newX < 0 || newX >= BOARD_WIDTH || newY >= BOARD_HEIGHT || newY < 0) {
                        return false
                    }
                    if (newY >= 0 && board[newY][newX] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun clearLines() {
        val newBoard = board.toMutableList()
        var linesCleared = 0
        for (y in board.indices.reversed()) {
            if (board[y].all { it != 0 }) {
                newBoard.removeAt(y)
                linesCleared++
            }
        }
        repeat(linesCleared) {
            newBoard.add(0, IntArray(BOARD_WIDTH))
        }
        board = newBoard.toTypedArray()
        score += linesCleared * 100
    }

    fun placePiece() {
        for (y in currentPiece.shape.indices) {
            for (x in currentPiece.shape[y].indices) {
                if (currentPiece.shape[y][x] == 1) {
                    board[currentPiece.y + y][currentPiece.x + x] = colors.indexOf(currentPiece.color) + 1
                }
            }
        }
        clearLines()
        currentPiece = randomPiece()
        if (!isValidPosition(currentPiece)) {
            gameOver = true
        }
    }

    fun movePiece(dx: Int, dy: Int) {
        if (!gameOver) {
            val newPiece = currentPiece.copy(x = currentPiece.x + dx, y = currentPiece.y + dy)
            if (isValidPosition(newPiece)) {
                currentPiece = newPiece
            } else if (dy > 0) {
                placePiece()
            }
        }
    }

    fun rotatePiece() {
        if (!gameOver) {
            val shape = currentPiece.shape
            val newShape = List(shape[0].size) { y ->
                List(shape.size) { x ->
                    shape[shape.size - 1 - x][y]
                }
            }
            val newPiece = currentPiece.copy(shape = newShape)
            if (isValidPosition(newPiece)) {
                currentPiece = newPiece
            }
        }
    }

    LaunchedEffect(key1 = gameOver) {
        while (!gameOver) {
            delay(500)
            movePiece(0, 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameBoard(board, currentPiece, Modifier.fillMaxSize())

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
                if (gameOver) {
                    Text("Game Over", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                }
                Text("Score: $score", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { movePiece(-1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                    }
                    Button(onClick = { rotatePiece() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rotate")
                    }
                    Button(onClick = { movePiece(1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                    }
                }
                Button(onClick = { movePiece(0, 1) }) {
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

fun randomPiece(): Piece {
    val index = Random.nextInt(shapes.size)
    return Piece(
        shape = shapes[index],
        color = colors[index],
        x = BOARD_WIDTH / 2 - 1,
        y = 0
    )
}
