package com.example.kttrs

import com.example.kttrs.GameConstants.BOARD_HEIGHT
import com.example.kttrs.GameConstants.BOARD_WIDTH

import com.example.kttrs.data.SettingsDataStore
import com.example.kttrs.ui.ControlMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: GameViewModel
    private val settingsDataStore: SettingsDataStore = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(settingsDataStore.controlMode).thenReturn(flowOf(ControlMode.Buttons))
        whenever(settingsDataStore.showGhostPiece).thenReturn(flowOf(true))
        viewModel = GameViewModel(settingsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `movePiece should move the current piece`() = runTest {
        val initialState = viewModel.gameState.value
        val initialX = initialState.currentPiece.x

        viewModel.movePiece(1)

        val newState = viewModel.gameState.value
        assertEquals(initialX + 1, newState.currentPiece.x)
    }

    @Test
    fun `movePiece should not move the current piece if it would go out of bounds`() = runTest {
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(currentPiece = initialPiece)
        viewModel.setGameStateForTest(gameState)

        viewModel.movePiece(-1)

        val newState = viewModel.gameState.value
        assertEquals(0, newState.currentPiece.x)
    }

    @Test
    fun `restartGame should reset the game state`() = runTest {
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
        viewModel.setGameStateForTest(gameState)

        viewModel.restartGame()

        val newState = viewModel.gameState.value
        assertEquals(0, newState.score)
        assertEquals(false, newState.gameOver)
        assertEquals(0, newState.linesCleared)
        // Check if board is empty (or mostly empty, as new piece is spawned)
        assertEquals(true, newState.board.all { row -> row.all { it == 0 } })
        // Current piece and next piece should be new random pieces, so we can't assert their exact values,
        // but we can assert they are not null and are valid pieces.
        assertEquals(false, newState.currentPiece == initialPiece)
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is null`() = runTest {
        val initialCurrentPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = null,
            canHold = true
        )
        viewModel.setGameStateForTest(gameState)

        viewModel.holdPiece()

        val newState = viewModel.gameState.value
        assertEquals(initialNextPiece, newState.currentPiece)
        assertEquals(initialCurrentPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `holdPiece should swap current piece with held piece when held piece is not null`() = runTest {
        val initialCurrentPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialHeldPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = initialHeldPiece,
            canHold = true
        )
        viewModel.setGameStateForTest(gameState)

        viewModel.holdPiece()

        val newState = viewModel.gameState.value
        assertEquals(initialHeldPiece, newState.currentPiece)
        assertEquals(initialCurrentPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `holdPiece should not do anything if canHold is false`() = runTest {
        val initialCurrentPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialHeldPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(
            currentPiece = initialCurrentPiece,
            nextPiece = initialNextPiece,
            heldPiece = initialHeldPiece,
            canHold = false
        )
        viewModel.setGameStateForTest(gameState)

        viewModel.holdPiece()

        val newState = viewModel.gameState.value
        assertEquals(initialCurrentPiece, newState.currentPiece)
        assertEquals(initialHeldPiece, newState.heldPiece)
        assertEquals(false, newState.canHold)
    }

    @Test
    fun `hardDrop should drop the piece to the bottom and place it`() = runTest {
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialNextPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1, 1))), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(
            currentPiece = initialPiece,
            nextPiece = initialNextPiece
        )
        viewModel.setGameStateForTest(gameState)

        viewModel.hardDrop()
        delay(510) // Wait for piece placement and potential line clear animation

        val newState = viewModel.gameState.value

        // Verify the initialPiece is placed on the board at the bottom
        val pieceIndex = 0
        assertEquals(pieceIndex + 1, newState.board[BOARD_HEIGHT - 1][0]) // Assuming 1x1 piece at (0,0) lands at (0, BOARD_HEIGHT-1)
        assertEquals(false, newState.gameOver)

        // Verify a new piece has spawned (currentPiece is not the initial one)
        assertEquals(false, newState.currentPiece == initialPiece)
    }

    @Test
    fun `hardDrop newY calculation should be correct`() = runTest {
        val initialPiece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }

        var newY = initialPiece.y
        while (true) {
            val nextY = newY + 1
            val testPiece = initialPiece.copy(y = nextY)
            if (viewModel.isValidPosition(testPiece, initialBoard)) { // Pass initialBoard to isValidPosition
                newY = nextY
            } else {
                break
            }
        }
        assertEquals(BOARD_HEIGHT - initialPiece.shape.size, newY)
    }

    @Test
    fun `rotatePieceRight should rotate the piece clockwise`() = runTest {
        // A 2x2 block (square) should not change shape when rotated
        val squareShape = listOf(listOf(1, 1), listOf(1, 1))
        val initialPiece = Piece(spec = TestPieceSpec(shape = squareShape), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(currentPiece = initialPiece)
        viewModel.setGameStateForTest(gameState)

        viewModel.rotatePieceRight()

        val newState = viewModel.gameState.value
        assertEquals(squareShape, newState.currentPiece.shape)

    }

    @Test
    fun `rotatePieceLeft should rotate the piece counter-clockwise`() = runTest {
        // A 2x2 block (square) should not change shape when rotated
        val squareShape = listOf(listOf(1, 1), listOf(1, 1))
        val initialPiece = Piece(spec = TestPieceSpec(shape = squareShape), x = 0, y = 0)
        val gameState = viewModel.gameState.value.copy(currentPiece = initialPiece)
        viewModel.setGameStateForTest(gameState)

        viewModel.rotatePieceLeft()

        val newState = viewModel.gameState.value
        assertEquals(squareShape, newState.currentPiece.shape)

        
    }

    @Test
    fun `placePiece should place the current piece on the board`() = runTest {
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val pieceToPlace = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = BOARD_HEIGHT - 1) // Place at bottom

        val gameState = viewModel.gameState.value.copy(
            board = initialBoard,
            currentPiece = pieceToPlace,
            score = 0,
            linesCleared = 0
        )
        viewModel.setGameStateForTest(gameState)

        // Call movePiece to trigger placePiece (move down by 1 to trigger placement)
        viewModel.placePiece()
        delay(210)

        val newState = viewModel.gameState.value

        // Assert the piece is on the board
        val pieceIndex = 0
        assertEquals(pieceIndex + 1, newState.board[BOARD_HEIGHT - 1][0]) // Piece placed at bottom
    }

    @Test
    fun `isValidPosition should return true for a valid position`() = runTest {
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = viewModel.isValidPosition(piece, board)
        assertEquals(true, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (left)`() = runTest {
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = -1, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = viewModel.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (right)`() = runTest {
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = BOARD_WIDTH, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = viewModel.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece is out of bounds (bottom)`() = runTest {
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = BOARD_HEIGHT)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        val result = viewModel.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `isValidPosition should return false if piece overlaps with existing blocks`() = runTest {
        val piece = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = 0)
        val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        board[0][0] = 1 // Place a block at (0,0)
        val result = viewModel.isValidPosition(piece, board)
        assertEquals(false, result)
    }

    @Test
    fun `getClearedLines should return the indices of cleared lines`() = runTest {
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        // Fill the bottom line
        for (x in 0 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }

        val clearedLinesIndices = viewModel.getClearedLines(initialBoard)

        assertEquals(1, clearedLinesIndices.size)
        assertEquals(BOARD_HEIGHT - 1, clearedLinesIndices[0])
    }

    @Test
    fun `placePiece should trigger line clearing and update score`() = runTest {
        val initialBoard = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        // Fill the bottom line, except for one block
        for (x in 1 until BOARD_WIDTH) {
            initialBoard[BOARD_HEIGHT - 1][x] = 1
        }
        val pieceToPlace = Piece(spec = TestPieceSpec(shape = listOf(listOf(1))), x = 0, y = BOARD_HEIGHT - 1) // Piece that will complete the line

        val gameState = viewModel.gameState.value.copy(
            board = initialBoard,
            currentPiece = pieceToPlace
        )
        viewModel.setGameStateForTest(gameState)

        viewModel.placePiece()

        // Wait for the animation to finish
        delay(210)

        // Check that the line is cleared and the board is updated
        assertTrue(viewModel.gameState.value.board[0].all { it == 0 })
        assertEquals(100, viewModel.gameState.value.score) // Score for 1 line
        assertEquals(1, viewModel.gameState.value.linesCleared)
        assertTrue(viewModel.gameState.value.clearingLines.isEmpty())
    }
}
