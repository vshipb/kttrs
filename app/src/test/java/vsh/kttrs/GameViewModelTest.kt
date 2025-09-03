package vsh.kttrs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import vsh.kttrs.data.SettingsDataStore
import vsh.kttrs.model.GameConstants.BOARD_HEIGHT
import vsh.kttrs.model.GameConstants.BOARD_WIDTH
import vsh.kttrs.model.GameState
import vsh.kttrs.model.GameViewModel
import vsh.kttrs.model.Piece
import vsh.kttrs.model.PieceType
import vsh.kttrs.model.shape
import vsh.kttrs.model.type
import vsh.kttrs.ui.ControlMode

@ExperimentalCoroutinesApi
class GameViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val settingsDataStore: SettingsDataStore = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(settingsDataStore.controlMode).thenReturn(flowOf(ControlMode.Buttons))
        whenever(settingsDataStore.showGhostPiece).thenReturn(flowOf(true))
        whenever(settingsDataStore.highScore).thenReturn(flowOf(0)) // Mock high score
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun runTestAndCleanup(
        testBody: suspend TestScope.(GameViewModel) -> Unit
    ): TestResult = runTest(context = testDispatcher) {
        val model = GameViewModel(settingsDataStore, testDispatcher)
        try {
             testBody(model)
        } finally {
            model.gameJob?.cancelAndJoin()
        }
    }
    @Test
    fun `movePiece should move the current piece`() = runTestAndCleanup { model ->
        val initialX = model.gameState.value.currentPiece.x

        model.movePiece(1)

        assertEquals(initialX + 1, model.gameState.value.currentPiece.x)
    }

    @Test
    fun `movePiece should not move the current piece if it would go out of bounds`() = runTestAndCleanup { model ->
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = model.gameState.value.copy(currentPiece = initialPiece)
        model.setGameStateForTest(gameState)

        model.movePiece(-1)

        assertEquals(0, model.gameState.value.currentPiece.x)
    }

    @Test
    fun `restartGame should reset the game state`() = runTestAndCleanup { model ->
        // Change some state to ensure it resets
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 5, y = 5)
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        initialBoard[0][0] = 1 // Place something on the board
        val gameState = GameState(
            board = initialBoard,
            currentPiece = initialPiece,
            nextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0),
            score = 100,
            gameOver = true,
            linesCleared = 10
        )
        model.setGameStateForTest(gameState) // This cancels previous gameJob

        model.restartGame() // This starts a new gameJob
        testDispatcher.scheduler.runCurrent() // Allow init tasks of restart to complete

        val newState = model.gameState.value
        assertEquals(0, newState.score)
        assertEquals(false, newState.gameOver)
        assertEquals(0, newState.linesCleared)
        assertEquals(true, newState.board.all { row -> row.all { it == 0 } })
        assertEquals(false, newState.currentPiece == initialPiece)
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is null`() = runTestAndCleanup { model ->
        val initialCurrentPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = model.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = null,
            canHold = true
        )
        model.setGameStateForTest(gameState)

        model.holdPiece()

        val newState = model.gameState.value
        assertEquals(initialNextPiece, newState.currentPiece)
        assertEquals(initialCurrentPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is not null`() = runTestAndCleanup { model ->
        val initialCurrentPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialHeldPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = model.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = initialHeldPiece,
            canHold = true
        )
        model.setGameStateForTest(gameState)

        model.holdPiece()
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(initialHeldPiece, newState.currentPiece)
        assertEquals(initialCurrentPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `holdPiece should not do anything if canHold is false`() = runTestAndCleanup { model ->
        val initialCurrentPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialHeldPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = model.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = initialHeldPiece,
            canHold = false
        )
        model.setGameStateForTest(gameState)

        model.holdPiece()
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(initialCurrentPiece, newState.currentPiece)
        assertEquals(initialHeldPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `hardDrop should drop the piece to the bottom and place it`() = runTestAndCleanup { model ->
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1, 1))), x = 0, y = 0)
        val gameState = model.gameState.value.copy(
            currentPiece = initialPiece,
            nextPiece = initialNextPiece
        )
        model.setGameStateForTest(gameState)

        model.hardDrop() // This calls placePiece which starts animations and gameJob
        testDispatcher.scheduler.runCurrent()
        // Advance time for line clear animations if any, or general processing
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()


        val newState = model.gameState.value
        val pieceIndex = 0
        assertEquals(pieceIndex + 1, newState.board[BOARD_HEIGHT - 1][0])
        assertEquals(false, newState.gameOver)
        assertEquals(false, newState.currentPiece == initialPiece)
    }

    @Test
    fun `hardDrop newY calculation should be correct`() = runTestAndCleanup { model ->
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }

        var newY = initialPiece.y
        while (true) {
            val nextY = newY + 1
            val testPiece = initialPiece.copy(y = nextY)
            if (model.isValidPosition(testPiece, initialBoard)) {
                newY = nextY
            } else {
                break
            }
        }
        assertEquals(BOARD_HEIGHT - initialPiece.shape.size, newY)
    }

    @Test
    fun `rotatePieceRight should rotate the piece clockwise`() = runTestAndCleanup { model ->
        val squareShape = listOf(listOf(1, 1), listOf(1, 1))
        val initialPiece = Piece(spec = TestPieceSpec(shape = squareShape), x = 0, y = 0)
        val gameState = model.gameState.value.copy(currentPiece = initialPiece)
        model.setGameStateForTest(gameState)

        model.rotatePieceRight()
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(squareShape, newState.currentPiece.shape)
    }

    @Test
    fun `rotatePieceLeft should rotate the piece counter-clockwise`() = runTestAndCleanup { model ->
        val squareShape = listOf(listOf(1, 1), listOf(1, 1))
        val initialPiece = Piece(spec = TestPieceSpec(shape = squareShape), x = 0, y = 0)
        val gameState = model.gameState.value.copy(currentPiece = initialPiece)
        model.setGameStateForTest(gameState)

        model.rotatePieceLeft()
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(squareShape, newState.currentPiece.shape)
    }

    @Test
    fun `placePiece should place the current piece on the board`() = runTestAndCleanup { model ->
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val pieceToPlace = Piece(
            spec = TestPieceSpec(shape = listOf(listOf(1))),
            x = 0,
            y = BOARD_HEIGHT - 1
        )

        val gameState = model.gameState.value.copy(
            board = initialBoard,
            currentPiece = pieceToPlace,
            score = 0,
            linesCleared = 0
        )
        model.setGameStateForTest(gameState)

        model.placePiece()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        val pieceIndex = 0
        assertEquals(pieceIndex + 1, newState.board[BOARD_HEIGHT - 1][0])
    }

    @Test
    fun `isValidPosition should return true for a valid position`() = runTestAndCleanup { model ->
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = model.isValidPosition(piece, board)
        assertEquals(true, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (left)`() = runTestAndCleanup { model ->
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = -1, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = model.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (right)`() = runTestAndCleanup { model ->
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = BOARD_WIDTH, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = model.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (bottom)`() = runTestAndCleanup { model ->
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = BOARD_HEIGHT)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = model.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece overlaps with existing blocks`() = runTestAndCleanup { model ->
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        board[0][0] = 1 // Place a block at (0,0)
        val result = model.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `getClearedLines should return the indices of cleared lines`() = runTestAndCleanup { model ->
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        for (x in 0 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }

        val clearedLinesIndices = model.getClearedLines(initialBoard)

        assertEquals(1, clearedLinesIndices.size)
        assertEquals(BOARD_HEIGHT - 1, clearedLinesIndices[0])
    }

    @Test
    fun `placePiece should trigger line clearing and update score`() = runTestAndCleanup { model ->
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        for (x in 1 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }
        val pieceToPlace = Piece(
            spec = TestPieceSpec(shape = listOf(listOf(1))),
            x = 0,
            y = BOARD_HEIGHT - 1
        )

        val gameState = model.gameState.value.copy(
            board = initialBoard,
            currentPiece = pieceToPlace
        )
        model.setGameStateForTest(gameState)

        model.placePiece()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()

        assertTrue(model.gameState.value.board[0].all { it == 0 })
        assertEquals(100, model.gameState.value.score)
        assertEquals(1, model.gameState.value.linesCleared)
        assertTrue(model.gameState.value.clearingLines.isEmpty())
    }

    @Test
    fun `updateBoard should save new high score when game is over and score is higher`() = runTestAndCleanup { model ->
        val spawnX = BOARD_WIDTH / 2 - 1
        val spawnY = 0
        val initialHighScore = 100
        val newHighScore = 200
        whenever(settingsDataStore.highScore).thenReturn(flowOf(initialHighScore))
        // Re-initialize viewModel for this specific test case's mock setup
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent() // Allow initial highScore collection

        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        board[spawnY][spawnX] = 1

        val pieceToPlace = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 5)
        val nextPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = spawnX, y = spawnY)

        val gameState = viewModel.gameState.value.copy(
            board = board,
            currentPiece = pieceToPlace,
            nextPiece = nextPiece,
            score = newHighScore
        )
        viewModel.setGameStateForTest(gameState) // Cancels previous gameJob
        testDispatcher.scheduler.runCurrent() // Allow state to settle

        viewModel.placePiece() // Starts new gameJob and potential saveHighScore
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1) // Allow animations/delays
        testDispatcher.scheduler.runCurrent() // Allow saveHighScore to be called

        verify(settingsDataStore).saveHighScore(newHighScore)
        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `updateBoard should not save high score when game is over and score is lower`() = runTestAndCleanup { model ->
        val spawnX = BOARD_WIDTH / 2 - 1
        val spawnY = 0
        val initialHighScore = 100
        val score = 50
        whenever(settingsDataStore.highScore).thenReturn(flowOf(initialHighScore))
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent()

        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        board[spawnY][spawnX] = 1

        val pieceToPlace = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 5)
        val nextPiece =
            Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = spawnX, y = spawnY)

        val gameState = viewModel.gameState.value.copy(
            board = board,
            currentPiece = pieceToPlace,
            nextPiece = nextPiece,
            score = score
        )
        viewModel.setGameStateForTest(gameState)
        testDispatcher.scheduler.runCurrent()

        viewModel.placePiece()
        testDispatcher.scheduler.runCurrent()


        verify(settingsDataStore, never()).saveHighScore(score)
        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `topScore should be initialized with persisted high score`() = runTest {
        // Arrange
        val persistedHighScore = 500
        whenever(settingsDataStore.highScore).thenReturn(flowOf(persistedHighScore))

        // Act
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)

        testDispatcher.scheduler.runCurrent()

        // Assert
        assertEquals(persistedHighScore, viewModel.topScore.value)

        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `topScore should be updated when game score surpasses it`() = runTest {
        // Arrange
        val initialHighScore = 100
        val newScore = 150
        // Ensure the initial highScore from settingsDataStore is collected first
        whenever(settingsDataStore.highScore).thenReturn(flowOf(initialHighScore))
        val viewModel = GameViewModel(settingsDataStore, testDispatcher) // Re-initialize for this test's specific mock
        testDispatcher.scheduler.runCurrent() // Collect initial highScore

        // Act
        // Simulate a game state change that updates the score
        val gameState = viewModel.gameState.value.copy(score = newScore)
        viewModel.setGameStateForTest(gameState) // This will also update the internal _gameState
        testDispatcher.scheduler.runCurrent() // Allow collectors listening to _gameState to run

        // Assert
        assertEquals(newScore, viewModel.topScore.value)

        // Очистка
        viewModel.gameJob?.cancelAndJoin()
    }


    @Test
    fun `T-Spin Mini Single should clear one line and add 800 to score`() = runTestAndCleanup { model ->
        // Arrange
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        // Create a T-Spin setup
        board[20] = intArrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)
        board[21] = intArrayOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1)

        val tPiece = Piece(spec = PieceType.T, x = -1, y = 19, rotation = 1)

        val gameState = model.gameState.value.copy(
            board = board,
            currentPiece = tPiece,
            score = 0,
            linesCleared = 0,
        )
        model.setGameStateForTest(gameState)
        printBoardState(model)
        model.rotatePieceLeft()
        printBoardState(model)
        model.placePiece()
        printBoardState(model)
        testDispatcher.scheduler.runCurrent() // Execute immediate tasks from placePiece
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent() // Execute tasks scheduled by those delays
        printBoardState(model)

        // Assert
        assertEquals(800, model.gameState.value.score)
        assertEquals(1, model.gameState.value.linesCleared)
    }

    @Test
    fun `T-Spin Single should clear one line and add something to score`() = runTestAndCleanup { model ->
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }

        board[19] = intArrayOf(0,1,0,0,0,0,0,0,0,0)
        board[20] = intArrayOf(1,1,0,0,0,1,1,1,1,1)
        board[21] = intArrayOf(0,1,1,0,0,0,0,0,0,0)

        val tPieceInitial = Piece(
            spec = PieceType.T,
            x = 2,
            y = 19,
            rotation = 1
        )

        val initialGameState = model.gameState.value.copy(
            board = board.map { it.clone() }.toTypedArray(),
            currentPiece = tPieceInitial,
            score = 0,
            linesCleared = 0
        )
        model.setGameStateForTest(initialGameState)
        printBoardState(model)

        model.rotatePieceLeft()
        printBoardState(model)
        model.placePiece()
        printBoardState(model)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()
        printBoardState(model)

        val finalState = model.gameState.value
        assertEquals( 1, finalState.linesCleared)
        assertEquals(100, finalState.score) // TODO: Check actual T-Spin single score
    }

    private fun printBoardState(model: GameViewModel, message: String = "Board State:") {
        val board = model.gameState.value.board
        val currentPiece = model.gameState.value.currentPiece
        val tempBoard = board.map { it.clone() }.toTypedArray()

        // Overlay current piece on the board
        val shape = currentPiece.shape
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] == 1) {
                    val boardX = currentPiece.x + x
                    val boardY = currentPiece.y + y
                    if (boardX >= 0 && boardX < BOARD_WIDTH && boardY >= 0 && boardY < BOARD_HEIGHT) {
                        tempBoard[boardY][boardX] = currentPiece.type?.ordinal?.plus(1) ?: 0 // Use piece type ordinal + 1 for occupied cells
                    }
                }
            }
        }

        println(message)
        for (y in tempBoard.indices) {
            for (x in tempBoard[y].indices) {
                val cellValue = tempBoard[y][x]
                if (cellValue == 0) {
                    print(". ")
                } else {
                    // Map piece type ordinal to a letter
                    val pieceChar = PieceType.entries[cellValue - 1].name.lowercase().first()
                    // Check if this cell is part of the current piece
                    var isCurrentPieceCell = false
                    for (py in shape.indices) {
                        for (px in shape[py].indices) {
                            if (shape[py][px] == 1) {
                                val boardX = currentPiece.x + px
                                val boardY = currentPiece.y + py
                                if (boardX == x && boardY == y) {
                                    isCurrentPieceCell = true
                                    break
                                }
                            }
                        }
                        if (isCurrentPieceCell) break
                    }

                    if (isCurrentPieceCell) {
                        print("${pieceChar.uppercaseChar()} ")
                    } else {
                        print("$pieceChar ")
                    }
                }
            }
            println()
        }
        println("Piece X: ${currentPiece.x}, Y: ${currentPiece.y}, Rotation: ${currentPiece.rotation}")
        println("--------------------")
    }
}
