package vsh.kttrs

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
    private val settingsDataStore: SettingsDataStore = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsDataStore.controlMode } returns flowOf(ControlMode.Buttons)
        every { settingsDataStore.showGhostPiece } returns flowOf(true)
        every { settingsDataStore.highScore } returns flowOf(0) // Mock high score
        coEvery { settingsDataStore.saveHighScore(any()) } returns Unit // Added this line
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
        val gameState = model.gameState.value.copy(currentPiece = testPiece())
        model.setGameStateForTest(gameState)
        model.movePiece(-1)
        assertEquals(0, model.gameState.value.currentPiece.x)
    }

    @Test
    fun `restartGame should reset the game state`() = runTestAndCleanup { model ->
        val initialBoard = emptyBoard()
        initialBoard[0][0] = 1
        val gameState = GameState(
            board = initialBoard,
            currentPiece = testPiece(x = 5, y = 5),
            nextPiece = testPiece(),
            score = 100,
            gameOver = true,
            linesCleared = 10
        )
        model.setGameStateForTest(gameState)

        model.restartGame()
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(0, newState.score)
        assertFalse(newState.gameOver)
        assertEquals(0, newState.linesCleared)
        assertTrue(newState.board.all { row -> row.all { it == 0 } })
        assertFalse(newState.currentPiece == testPiece())
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is null`() = runTestAndCleanup { model ->
        val initialCurrentPiece = testPiece()
        val initialNextPiece = testPiece()
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
        assertFalse(newState.canHold)
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is not null`() = runTestAndCleanup { model ->
        val initialCurrentPiece = testPiece()
        val initialNextPiece = testPiece()
        val initialHeldPiece = testPiece()
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
        assertFalse(newState.canHold)
    }

    @Test
    fun `holdPiece should not do anything if canHold is false`() = runTestAndCleanup { model ->
        val initialCurrentPiece = testPiece()
        val initialNextPiece = testPiece()
        val initialHeldPiece = testPiece()
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
        assertFalse(newState.canHold)
    }

    @Test
    fun `hardDrop should drop the piece to the bottom and place it`() = runTestAndCleanup { model ->
        val gameState = model.gameState.value.copy(
            currentPiece = testPiece(),
            nextPiece = testPiece()
        )
        model.setGameStateForTest(gameState)
        model.hardDrop()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()

        val newState = model.gameState.value
        assertEquals(1, newState.board[BOARD_HEIGHT - 1][0])
        assertFalse(newState.gameOver)
    }

    @Test
    fun `hardDrop newY calculation should be correct`() = runTestAndCleanup { model ->
        val initialPiece = testPiece()
        val initialBoard = emptyBoard()
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
        val initialPiece = testPiece(shape = squareShape)
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
        val initialPiece = testPiece(shape = squareShape)
        val gameState = model.gameState.value.copy(currentPiece = initialPiece)
        model.setGameStateForTest(gameState)
        model.rotatePieceLeft()
        testDispatcher.scheduler.runCurrent()
        val newState = model.gameState.value
        assertEquals(squareShape, newState.currentPiece.shape)
    }

    @Test
    fun `placePiece should place the current piece on the board`() = runTestAndCleanup { model ->
        val initialBoard = emptyBoard()
        val pieceToPlace = testPiece(x = 0, y = BOARD_HEIGHT - 1)
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
        assertTrue(model.isValidPosition(testPiece(),emptyBoard()))
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (left)`() = runTestAndCleanup { model ->
        assertFalse(model.isValidPosition(testPiece(x = -1), emptyBoard()))
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (right)`() = runTestAndCleanup { model ->
        assertFalse(model.isValidPosition(testPiece(x = BOARD_WIDTH), emptyBoard()))
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (bottom)`() = runTestAndCleanup { model ->
        assertFalse(model.isValidPosition(testPiece(y = BOARD_HEIGHT), emptyBoard()))
    }

    @Test
    fun `isValidPosition should return false if piece overlaps with existing blocks`() = runTestAndCleanup { model ->
        val board = emptyBoard()
        board[0][0] = 1 // Place a block at (0,0)
        assertFalse(model.isValidPosition(testPiece(), board))
    }

    @Test
    fun `getClearedLines should return the indices of cleared lines`() = runTestAndCleanup { model ->
        val initialBoard = emptyBoard()
        for (x in 0 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }
        val clearedLinesIndices = model.getClearedLines(initialBoard)
        assertEquals(1, clearedLinesIndices.size)
        assertEquals(BOARD_HEIGHT - 1, clearedLinesIndices[0])
    }

    @Test
    fun `placePiece should trigger line clearing and update score`() = runTestAndCleanup { model ->
        val initialBoard = emptyBoard()
        for (x in 1 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }
        val gameState = model.gameState.value.copy(
            board = initialBoard,
            currentPiece = testPiece(x = 0, y = BOARD_HEIGHT - 1)
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
        every { settingsDataStore.highScore } returns flowOf(initialHighScore)
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent() // Allow initial highScore collection
        val board = emptyBoard()
        board[spawnY][spawnX] = 1
        val gameState = viewModel.gameState.value.copy(
            board = board,
            currentPiece = testPiece(x = 0, y = 5),
            nextPiece = testPiece(x = spawnX, y = spawnY),
            score = newHighScore
        )
        viewModel.setGameStateForTest(gameState) // Cancels previous gameJob
        testDispatcher.scheduler.runCurrent() // Allow state to settle
        viewModel.placePiece() // Starts new gameJob and potential saveHighScore
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1) // Allow animations/delays
        testDispatcher.scheduler.runCurrent() // Allow saveHighScore to be called
        coVerify { settingsDataStore.saveHighScore(newHighScore) }
        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `updateBoard should not save high score when game is over and score is lower`() = runTestAndCleanup { model ->
        val spawnX = BOARD_WIDTH / 2 - 1
        val spawnY = 0
        val initialHighScore = 100
        val score = 50
        every { settingsDataStore.highScore } returns flowOf(initialHighScore)
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        val board = emptyBoard()
        board[spawnY][spawnX] = 1

        val gameState = viewModel.gameState.value.copy(
            board = board,
            currentPiece = testPiece(x = 0, y = 5),
            nextPiece = testPiece(x = spawnX, y = spawnY),
            score = score
        )
        viewModel.setGameStateForTest(gameState)
        testDispatcher.scheduler.runCurrent()
        viewModel.placePiece()
        testDispatcher.scheduler.runCurrent()
        coVerify(exactly = 0) { settingsDataStore.saveHighScore(score) }
        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `topScore should be initialized with persisted high score`() = runTest {
        val persistedHighScore = 500
        every { settingsDataStore.highScore } returns flowOf(persistedHighScore)
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        assertEquals(persistedHighScore, viewModel.topScore.value)
        viewModel.gameJob?.cancelAndJoin()
    }

    @Test
    fun `topScore should be updated when game score surpasses it`() = runTest {
        val newScore = 150
        every { settingsDataStore.highScore } returns flowOf(100)
        val viewModel = GameViewModel(settingsDataStore, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        val gameState = viewModel.gameState.value.copy(score = newScore)
        viewModel.setGameStateForTest(gameState)
        testDispatcher.scheduler.runCurrent()
        assertEquals(newScore, viewModel.topScore.value)
        viewModel.gameJob?.cancelAndJoin()
    }


    @Test
    fun `T-Spin Mini Single should clear one line and add 800 to score`() = runTestAndCleanup { model ->
        val board = emptyBoard()
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
        model.printBoardState()
        model.rotatePieceLeft()
        model.printBoardState()
        model.placePiece()
        model.printBoardState()
        testDispatcher.scheduler.runCurrent() // Execute immediate tasks from placePiece
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent() // Execute tasks scheduled by those delays
        model.printBoardState()
        assertEquals(800, model.gameState.value.score)
        assertEquals(1, model.gameState.value.linesCleared)
    }

    @Test
    fun `T-Spin Single should clear one line and add something to score`() = runTestAndCleanup { model ->
        val board = emptyBoard()
        board[19] = intArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0)
        board[20] = intArrayOf(1, 1, 0, 0, 0, 1, 1, 1, 1, 1)
        board[21] = intArrayOf(0, 1, 1, 0, 0, 0, 0, 0, 0, 0)
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
        model.printBoardState()
        model.rotatePieceLeft()
        model.printBoardState()
        model.placePiece()
        model.printBoardState()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(GameViewModel.LINE_CLEAR_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()
        model.printBoardState()
        val finalState = model.gameState.value
        assertEquals( 1, finalState.linesCleared)
        assertEquals(100, finalState.score) // TODO: Check actual T-Spin single score
    }

    private fun GameViewModel.printBoardState(message: String = "Board State:") {
        val board = gameState.value.board
        val currentPiece = gameState.value.currentPiece
        val boardChars = board.map { ints ->  ints.map {
            if (it == 0) '.'
            else PieceType.entries[it - 1].name.first().lowercaseChar()}.toCharArray() }.toTypedArray()
        val shape = currentPiece.shape
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] == 1) {
                    val boardX = currentPiece.x + x
                    val boardY = currentPiece.y + y
                    if (boardX in 0 .. BOARD_WIDTH && boardY in 0 .. BOARD_HEIGHT) {
                        boardChars[boardY][boardX] = currentPiece.type?.name?.first() ?: 'X'
                    }
                }
            }
        }
        println(message)
        boardChars.forEach {
            println(it.joinToString(" "))
        }
        println("Piece X: ${currentPiece.x}, Y: ${currentPiece.y}, Rotation: ${currentPiece.rotation}")
        println("--------------------")
    }

    fun testPiece(x: Int = 0, y: Int = 0, rotation: Int = 0, shape: List<List<Int>> = listOf(listOf(1)))  = Piece(TestPieceSpec(shape), x, y, rotation)

    fun emptyBoard() = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
}
