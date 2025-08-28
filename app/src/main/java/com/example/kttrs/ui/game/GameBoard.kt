package com.example.kttrs.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.kttrs.GameConstants
import com.example.kttrs.Piece
import com.example.kttrs.PieceType
import com.example.kttrs.drawableResId
import com.example.kttrs.shape
import kotlin.math.min

@Composable
fun GameBoard(
    board: Array<IntArray>,
    currentPiece: Piece,
    ghostPiece: Piece?,
    showGhostPiece: Boolean,
    clearingLines: List<Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Load ImageBitmaps outside Canvas scope
    val blockImages = remember(PieceType.values()) {
        PieceType.values().map {
            ImageBitmap.Companion.imageResource(
                context.resources,
                it.drawableResId
            )
        }
    }
    val currentPieceImageBitmap = remember(currentPiece.drawableResId) {
        ImageBitmap.Companion.imageResource(context.resources, currentPiece.drawableResId)
    }
    val ghostPieceImageBitmap = remember(ghostPiece?.drawableResId) {
        ghostPiece?.drawableResId?.let {
            ImageBitmap.Companion.imageResource(
                context.resources,
                it
            )
        }
    }

    Canvas(
        modifier = modifier.background(
            Brush.Companion.verticalGradient(
                listOf(
                    Color.Companion.DarkGray,
                    Color.Companion.Black
                )
            )
        )
    ) {
        val cellSize =
            min(size.width / GameConstants.BOARD_WIDTH, size.height / GameConstants.BOARD_HEIGHT)
        val cellSizeInt = cellSize.toInt()
        // Draw board
        for (y in board.indices) {
            for (x in board[y].indices) {
                if (board[y][x] != 0) {
                    if (clearingLines.contains(y)) {
                        drawRect(
                            color = Color.Companion.White,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    } else {
                        val imageBitmap = blockImages[board[y][x] - 1]
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset((x * cellSize).toInt(), (y * cellSize).toInt()),
                            dstSize = IntSize(cellSizeInt, cellSizeInt)
                        )
                    }
                }
            }
        }
        // Draw ghost piece
        if (showGhostPiece && ghostPiece != null && ghostPieceImageBitmap != null) {
            for (y in ghostPiece.shape.indices) {
                for (x in ghostPiece.shape[y].indices) {
                    if (ghostPiece.shape[y][x] == 1) {
                        drawImage(
                            image = ghostPieceImageBitmap,
                            dstOffset = IntOffset(
                                ((ghostPiece.x + x) * cellSize).toInt(),
                                ((ghostPiece.y + y) * cellSize).toInt()
                            ),
                            dstSize = IntSize(cellSizeInt, cellSizeInt),
                            alpha = 0.3f // For ghost effect
                        )
                    }
                }
            }
        }
        // Draw current piece
        if (clearingLines.isEmpty()) {
            for (y in currentPiece.shape.indices) {
                for (x in currentPiece.shape[y].indices) {
                    if (currentPiece.shape[y][x] == 1) {
                        drawImage(
                            image = currentPieceImageBitmap,
                            dstOffset = IntOffset(
                                ((currentPiece.x + x) * cellSize).toInt(),
                                ((currentPiece.y + y) * cellSize).toInt()
                            ),
                            dstSize = IntSize(cellSizeInt, cellSizeInt)
                        )
                    }
                }
            }
        }
    }
}