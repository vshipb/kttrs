package com.example.kttrs

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kttrs.data.SettingsDataStore
import com.example.kttrs.ui.ControlMode
import com.example.kttrs.GameConstants.BOARD_HEIGHT
import com.example.kttrs.GameConstants.BOARD_WIDTH

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
    val ghostPiece: Piece? = null,
    val clearingLines: List<Int> = emptyList()
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
        if (clearingLines != other.clearingLines) return false

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
        result = 31 * result + clearingLines.hashCode()
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
    private var lastMoveIsRotation = false
    private var lockDelayJob: Job? = null

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
                    // Gravity tick
                    val piece = _gameState.value.currentPiece
                    val newPiece = piece.copy(y = piece.y + 1)
                    if (isValidPosition(newPiece)) {
                        _gameState.value = _gameState.value.copy(currentPiece = newPiece)
                        lockDelayJob?.cancel() // Cancel any active lock delay if piece moved down
                    } else {
                        // Cannot move down, start/continue lock delay
                        startLockDelay()
                    }
                }
            }
        }
    }

    private fun startLockDelay() {
        if (lockDelayJob?.isActive == true) return // Lock delay already active

        lockDelayJob = viewModelScope.launch {
            delay(500) // Lock delay duration (e.g., 500ms)
            // Check if piece is still on ground before placing
            val piece = _gameState.value.currentPiece
            if (!isValidPosition(piece.copy(y = piece.y + 1))) {
                placePiece()
            }
            lockDelayJob = null
        }
    }

    fun movePiece(dx: Int) { // Player horizontal move
        if (_gameState.value.gameOver) return

        val piece = _gameState.value.currentPiece
        val newPiece = piece.copy(x = piece.x + dx)

        if (isValidPosition(newPiece)) {
            _gameState.value = _gameState.value.copy(currentPiece = newPiece)
            // Reset lock delay if piece is on ground after move
            if (!isValidPosition(newPiece.copy(y = newPiece.y + 1))) {
                lockDelayJob?.cancel()
                startLockDelay()
            } else {
                // Piece moved off ground, cancel lock delay
                lockDelayJob?.cancel()
            }
        }
    }

    fun softDrop() {
        if (_gameState.value.gameOver) return

        val newPiece = _gameState.value.currentPiece.copy(y = _gameState.value.currentPiece.y + 1)
        if (isValidPosition(newPiece)) {
            lastMoveIsRotation = false
            _gameState.value = _gameState.value.copy(currentPiece = newPiece)
            // Reset lock delay if piece is on ground after move
            if (!isValidPosition(newPiece.copy(y = newPiece.y + 1))) {
                lockDelayJob?.cancel()
                startLockDelay()
            } else {
                // Piece moved off ground, cancel lock delay
                lockDelayJob?.cancel()
            }
        } else {
            // Cannot move down, start/continue lock delay
            startLockDelay()
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
        lastMoveIsRotation = false
        // After hard drop, the piece is on the ground, so start lock delay
        startLockDelay()
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

        val piece = _gameState.value.currentPiece
        if (piece.type == PieceType.O) return // O piece doesn't rotate

        val oldRotation = piece.rotation
        val newRotation = if (clockwise) (oldRotation + 1) % 4 else (oldRotation + 3) % 4

        val kickData = if (piece.type == PieceType.I) {
            GameConstants.iKickData
        } else {
            GameConstants.commonKickData
        }

        val kicks = kickData[oldRotation to newRotation] ?: emptyList()

        for (kick in kicks) {
            val newPiece = piece.copy(
                x = piece.x + kick.first,
                y = piece.y - kick.second, // SRS y-axis is inverted from our board y-axis
                rotation = newRotation
            )
            if (isValidPosition(newPiece)) {
                lastMoveIsRotation = true
                _gameState.value = _gameState.value.copy(currentPiece = newPiece)

                // Reset lock delay
                lockDelayJob?.cancel()
                val isNewPieceOnGround = !isValidPosition(newPiece.copy(y = newPiece.y + 1))
                if (isNewPieceOnGround) {
                    startLockDelay()
                }
                return
            }
        }
    }

    private fun isTSpin(piece: Piece, board: Array<IntArray>): Boolean {
        if (piece.type != PieceType.T) return false

        // T-Spin is defined by 3 of the 4 corners of the piece's 3x3 bounding box being occupied.
        // The center of the T piece is at x+1, y+1 of its local coordinates.
        val cx = piece.x + 1
        val cy = piece.y + 1

        val corners = listOf(
            Pair(cx - 1, cy - 1), // Top-left
            Pair(cx + 1, cy - 1), // Top-right
            Pair(cx - 1, cy + 1), // Bottom-left
            Pair(cx + 1, cy + 1)  // Bottom-right
        )

        var occupiedCorners = 0
        for (corner in corners) {
            val x = corner.first
            val y = corner.second
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT || board[y][x] != 0) {
                occupiedCorners++
            }
        }

        return occupiedCorners >= 3
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

        val piece = _gameState.value.currentPiece
        val board = _gameState.value.board

        val isTSpinMove = lastMoveIsRotation && isTSpin(piece, board)
        lastMoveIsRotation = false // Reset flag

        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] == 1) {
                    val pieceIndex = piece.type?.ordinal ?: 0
                    newBoard[piece.y + y][piece.x + x] = pieceIndex + 1
                }
            }
        }

        val clearedLinesIndices = getClearedLines(newBoard)
        val scoreToAdd: Int

        if (isTSpinMove) {
            scoreToAdd = when (clearedLinesIndices.size) {
                1 -> 800  // T-Spin Single
                2 -> 1200 // T-Spin Double
                else -> 400 // T-Spin
            }
        } else {
            scoreToAdd = when (clearedLinesIndices.size) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800 // Tetris
                else -> 0
            }
        }

        if (clearedLinesIndices.isNotEmpty() || scoreToAdd > 0) {
            viewModelScope.launch {
                _gameState.value = _gameState.value.copy(
                    board = newBoard,
                    clearingLines = clearedLinesIndices
                )
                delay(200)

                val boardAfterClearing = newBoard.filterIndexed { index, _ -> !clearedLinesIndices.contains(index) }.toTypedArray()
                val newRows = Array(clearedLinesIndices.size) { IntArray(BOARD_WIDTH) }
                val finalBoard = newRows + boardAfterClearing

                val newScore = _gameState.value.score + scoreToAdd
                val newLinesCleared = _gameState.value.linesCleared + clearedLinesIndices.size
                val newSpeed = 500L - (newLinesCleared / 10) * 50
                val newPiece = _gameState.value.nextPiece
                val isGameOver = !isValidPosition(newPiece, finalBoard)

                _gameState.value = _gameState.value.copy(
                    board = finalBoard,
                    score = newScore,
                    currentPiece = newPiece,
                    nextPiece = randomPiece(),
                    gameOver = _gameState.value.gameOver || isGameOver,
                    linesCleared = newLinesCleared,
                    gameSpeed = newSpeed.coerceAtLeast(100L),
                    canHold = true,
                    clearingLines = emptyList()
                )
            }
        } else {
            val newPiece = _gameState.value.nextPiece
            val isGameOver = !isValidPosition(newPiece, newBoard)
            _gameState.value = _gameState.value.copy(
                board = newBoard,
                currentPiece = newPiece,
                nextPiece = randomPiece(),
                gameOver = _gameState.value.gameOver || isGameOver,
                canHold = true
            )
        }
    }

    @VisibleForTesting
    internal fun getClearedLines(board: Array<IntArray>): List<Int> {
        val clearedLinesIndices = mutableListOf<Int>()
        for (y in board.indices) {
            if (board[y].all { it != 0 }) {
                clearedLinesIndices.add(y)
            }
        }
        return clearedLinesIndices
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
        val pieceType = PieceType.values().random()
        return Piece(
            spec = pieceType,
            x = BOARD_WIDTH / 2 - 1,
            y = 0,
            rotation = 0
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