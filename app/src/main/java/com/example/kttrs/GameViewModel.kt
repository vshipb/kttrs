package com.example.kttrs

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameState(
    val board: Array<IntArray> = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) },
    val currentPiece: Piece,
    val nextPiece: Piece,
    val heldPiece: Piece? = null,
    val canHold: Boolean = true,
    val score: Int = 0,
    val gameOver: Boolean = false,
    val linesCleared: Int = 0,
    val gameSpeed: Long = 500L,
    val controlMode: ControlMode = ControlMode.Buttons,
    val ghostPiece: Piece? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentPiece != other.currentPiece) return false
        if (nextPiece != other.nextPiece) return false
        if (heldPiece != other.heldPiece) return false
        if (canHold != other.canHold) return false
        if (score != other.score) return false
        if (gameOver != other.gameOver) return false
        if (linesCleared != other.linesCleared) return false
        if (gameSpeed != other.gameSpeed) return false
        if (controlMode != other.controlMode) return false
        if (ghostPiece != other.ghostPiece) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPiece.hashCode()
        result = 31 * result + nextPiece.hashCode()
        result = 31 * result + (heldPiece?.hashCode() ?: 0)
        result = 31 * result + canHold.hashCode()
        result = 31 * result + score
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + linesCleared
        result = 31 * result + gameSpeed.hashCode()
        result = 31 * result + controlMode.hashCode()
        result = 31 * result + (ghostPiece?.hashCode() ?: 0)
        return result
    }
}

class GameViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState(currentPiece = randomPiece(), nextPiece = randomPiece()))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val showGhostPiece: StateFlow<Boolean> = settingsDataStore.showGhostPiece.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            settingsDataStore.controlMode.collect {
                _gameState.value = _gameState.value.copy(controlMode = it)
            }
        }
        viewModelScope.launch {
            _gameState.collectLatest { gameState ->
                _gameState.value = gameState.copy(ghostPiece = calculateGhostPiecePosition(gameState.currentPiece, gameState.board))
            }
        }
        startGameLoop()
    }

    fun restartGame() {
        _gameState.value = GameState(currentPiece = randomPiece(), nextPiece = randomPiece())
        startGameLoop()
    }

    @VisibleForTesting
    fun setGameStateForTest(gameState: GameState) {
        _gameState.value = gameState
    }

    fun setControlMode(controlMode: ControlMode) {
        viewModelScope.launch {
            settingsDataStore.saveControlMode(controlMode)
        }
    }

    fun saveShowGhostPiece(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveShowGhostPiece(show)
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

    fun holdPiece() {
        if (_gameState.value.gameOver || !_gameState.value.canHold) return

        val currentPiece = _gameState.value.currentPiece
        val heldPiece = _gameState.value.heldPiece

        if (heldPiece == null) {
            _gameState.value = _gameState.value.copy(
                currentPiece = _gameState.value.nextPiece,
                nextPiece = randomPiece(),
                heldPiece = currentPiece,
                canHold = false
            )
        } else {
            _gameState.value = _gameState.value.copy(
                currentPiece = heldPiece,
                heldPiece = currentPiece,
                canHold = false
            )
        }
    }

    fun hardDrop() {
        if (_gameState.value.gameOver) return

        var newY = _gameState.value.currentPiece.y
        while (true) {
            val nextY = newY + 1
            val newPiece = _gameState.value.currentPiece.copy(y = nextY)
            if (isValidPosition(newPiece)) {
                newY = nextY
            } else {
                break
            }
        }
        _gameState.value = _gameState.value.copy(currentPiece = _gameState.value.currentPiece.copy(y = newY))
        placePiece()
    }

    fun rotatePieceRight() {
        rotatePiece(true)
    }

    fun rotatePieceLeft() {
        rotatePiece(false)
    }

    @VisibleForTesting
    internal fun rotatePiece(clockwise: Boolean) {
        if (_gameState.value.gameOver) return

        val shape = _gameState.value.currentPiece.shape
        val newShape = if (clockwise) {
            List(shape[0].size) { y ->
                List(shape.size) { x ->
                    shape[shape.size - 1 - x][y]
                }
            }
        } else {
            List(shape[0].size) { y ->
                List(shape.size) { x ->
                    shape[x][shape[0].size - 1 - y]
                }
            }
        }

        val testOffsets = listOf(0, 1, -1, 2, -2)
        for (offset in testOffsets) {
            val newPiece = _gameState.value.currentPiece.copy(
                shape = newShape,
                x = _gameState.value.currentPiece.x + offset
            )
            if (isValidPosition(newPiece)) {
                _gameState.value = _gameState.value.copy(currentPiece = newPiece)
                return
            }
        }
    }

    @VisibleForTesting
    internal fun placePiece() {
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
        val newPiece = _gameState.value.nextPiece
        val isGameOver = !isValidPosition(newPiece, clearedBoard)

        _gameState.value = _gameState.value.copy(
            board = clearedBoard,
            score = newScore,
            currentPiece = newPiece,
            nextPiece = randomPiece(),
            gameOver = _gameState.value.gameOver || isGameOver,
            linesCleared = newLinesCleared,
            gameSpeed = newSpeed.coerceAtLeast(100L),
            canHold = true
        )
    }

    @VisibleForTesting
    internal fun clearLines(board: Array<IntArray>): Pair<Array<IntArray>, Int> {
        val newBoardRows = mutableListOf<IntArray>()
        var linesCleared = 0
        for (y in board.indices) {
            if (board[y].all { it != 0 }) {
                linesCleared++
            } else {
                newBoardRows.add(board[y])
            }
        }
        repeat(linesCleared) {
            newBoardRows.add(0, IntArray(BOARD_WIDTH))
        }
        return Pair(newBoardRows.toTypedArray(), linesCleared)
    }

    @VisibleForTesting
    internal fun isValidPosition(piece: Piece, board: Array<IntArray> = _gameState.value.board): Boolean {
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

    private fun calculateGhostPiecePosition(currentPiece: Piece, board: Array<IntArray>): Piece {
        var ghostY = currentPiece.y
        while (true) {
            val newPiece = currentPiece.copy(y = ghostY + 1)
            if (isValidPosition(newPiece, board)) {
                ghostY++
            } else {
                break
            }
        }
        return currentPiece.copy(y = ghostY)
    }
}
