
package com.example.kttrs

import androidx.compose.ui.graphics.Color
import com.example.kttrs.data.SettingsDataStore
import com.example.kttrs.ui.ControlMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

        viewModel.movePiece(1, 0)

        val newState = viewModel.gameState.value
        assertEquals(initialX + 1, newState.currentPiece.x)
    }

    @Test
    fun `movePiece should not move the current piece if it would go out of bounds`() = runTest {
        val initialPiece = Piece(listOf(listOf(1)), Color.Red, 0, 0)
        val gameState = viewModel.gameState.value.copy(currentPiece = initialPiece)
        viewModel.setGameStateForTest(gameState)

        viewModel.movePiece(-1, 0)

        val newState = viewModel.gameState.value
        assertEquals(0, newState.currentPiece.x)
    }
}
