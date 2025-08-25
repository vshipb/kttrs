package com.example.kttrs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kttrs.data.SettingsDataStore
import com.example.kttrs.ui.ControlMode
import com.example.kttrs.GameConstants.BOARD_HEIGHT
import com.example.kttrs.GameConstants.BOARD_WIDTH
import com.example.kttrs.GameConstants.colors
import com.example.kttrs.GameConstants.shapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameState(
    val board: Array<IntArray> = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) },
    val currentPiece: Piece,
    val score: Int = 0,
    val gameOver: Boolean = false,
    val linesCleared: Int = 0,
    val gameSpeed: Long = 500L,
    val controlMode: ControlMode = ControlMode.Buttons
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentPiece != other.currentPiece) return false
        if (score != other.score) return false
        if (gameOver != other.gameOver) return false
        if (linesCleared != other.linesCleared) return false
        if (gameSpeed != other.gameSpeed) return false
        if (controlMode != other.controlMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPiece.hashCode()
        result = 31 * result + score
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + linesCleared
        result = 31 * result + gameSpeed.hashCode()
        result = 31 * result + controlMode.hashCode()
        return result
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val _gameState = MutableStateFlow(GameState(currentPiece = randomPiece()))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            settingsDataStore.controlMode.collect {
                _gameState.value = _gameState.value.copy(controlMode = it)
            }
        }
        startGameLoop()
    }

    fun setControlMode(controlMode: ControlMode) {
        viewModelScope.launch {
            settingsDataStore.saveControlMode(controlMode)
        }
    }

    private fun startGameLoop() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(_gameState.value.gameSpeed)
                if (!_gameState.value.gameOver) {
                    movePiece(0, 1)
                }
            }
        }
    }

    fun movePiece(dx: Int, dy: Int) {
        if (_gameState.value.gameOver) return

        val newPiece = _gameState.value.currentPiece.copy(x = _gameState.value.currentPiece.x + dx, y = _gameState.value.currentPiece.y + dy)
        if (isValidPosition(newPiece)) {
            _gameState.value = _gameState.value.copy(currentPiece = newPiece)
        } else if (dy > 0) {
            placePiece()
        }
    }

    fun rotatePiece() {
        if (_gameState.value.gameOver) return

        // Standard 90-degree rotation
        val shape = _gameState.value.currentPiece.shape
        val newShape = List(shape[0].size) { y ->
            List(shape.size) { x ->
                shape[shape.size - 1 - x][y]
            }
        }

        // Wall kick implementation
        // Try to move the piece to a valid position after rotation.
        // This is a simplified version of the Super Rotation System (SRS).
        // We test the original position (offset 0), then 1 and -1, and finally 2 and -2.
        val testOffsets = listOf(0, 1, -1, 2, -2)
        for (offset in testOffsets) {
            val newPiece = _gameState.value.currentPiece.copy(
                shape = newShape,
                x = _gameState.value.currentPiece.x + offset
            )
            if (isValidPosition(newPiece)) {
                _gameState.value = _gameState.value.copy(currentPiece = newPiece)
                return // Rotation successful
            }
        }
    }

    private fun placePiece() {
        for (y in _gameState.value.currentPiece.shape.indices) {
            for (x in _gameState.value.currentPiece.shape[y].indices) {
                if (_gameState.value.currentPiece.shape[y][x] == 1) {
                    if (_gameState.value.currentPiece.y + y < 0) {
                        _gameState.value = _gameState.value.copy(gameOver = true)
                        return
                    }
                }
            }
        }

        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        for (y in _gameState.value.currentPiece.shape.indices) {
            for (x in _gameState.value.currentPiece.shape[y].indices) {
                if (_gameState.value.currentPiece.shape[y][x] == 1) {
                    newBoard[_gameState.value.currentPiece.y + y][_gameState.value.currentPiece.x + x] = colors.indexOf(_gameState.value.currentPiece.color) + 1
                }
            }
        }

        val (clearedBoard, linesCleared) = clearLines(newBoard)
        val newScore = _gameState.value.score + linesCleared * 100
        val newLinesCleared = _gameState.value.linesCleared + linesCleared
        val newSpeed = 500L - (newLinesCleared / 10) * 50
        val newPiece = randomPiece()
        val isGameOver = !isValidPosition(newPiece, clearedBoard)

        _gameState.value = _gameState.value.copy(
            board = clearedBoard,
            score = newScore,
            currentPiece = newPiece,
            gameOver = _gameState.value.gameOver || isGameOver,
            linesCleared = newLinesCleared,
            gameSpeed = newSpeed.coerceAtLeast(100L)
        )
    }

    private fun clearLines(board: Array<IntArray>): Pair<Array<IntArray>, Int> {
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
        return Pair(newBoard.toTypedArray(), linesCleared)
    }

    private fun isValidPosition(piece: Piece, board: Array<IntArray> = _gameState.value.board): Boolean {
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] == 1) {
                    val newX = piece.x + x
                    val newY = piece.y + y
                    if (newX < 0 || newX >= BOARD_WIDTH || newY >= BOARD_HEIGHT) {
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

    private fun randomPiece(): Piece {
        val index = Random.nextInt(shapes.size)
        return Piece(
            shape = shapes[index],
            color = colors[index],
            x = BOARD_WIDTH / 2 - 1,
            y = 0
        )
    }
}
