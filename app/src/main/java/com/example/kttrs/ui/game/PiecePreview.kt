package com.example.kttrs.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.kttrs.Piece
import kotlin.math.min

@Composable
fun PiecePreview(piece: Piece?, modifier: Modifier = Modifier.Companion) {
    Box(modifier = modifier) {
        if (piece != null) {
            val context = LocalContext.current
            val imageBitmap = remember(piece.drawableResId) {
                ImageBitmap.Companion.imageResource(context.resources, piece.drawableResId)
            }
            Canvas(modifier = Modifier.Companion.fillMaxSize()) {
                val cellSize = min(size.width / 4, size.height / 4)
                val cellSizeInt = cellSize.toInt()
                for (y in piece.shape.indices) {
                    for (x in piece.shape[y].indices) {
                        if (piece.shape[y][x] == 1) {
                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset(
                                    (x * cellSize).toInt(),
                                    (y * cellSize).toInt()
                                ),
                                dstSize = IntSize(cellSizeInt, cellSizeInt)
                            )
                        }
                    }
                }
            }
        }
    }
}