package com.example.kttrs

import androidx.compose.ui.graphics.Color

object GameConstants {
    const val BOARD_WIDTH = 10
    const val BOARD_HEIGHT = 22

    val pieceInfos = listOf(
        PieceInfo(
            shape = listOf(listOf(1, 1, 1, 1)), // I
            color = Color.Cyan,
            drawableResId = R.drawable.block_i
        ),
        PieceInfo(
            shape = listOf(listOf(1, 1), listOf(1, 1)), // O
            color = Color.Yellow,
            drawableResId = R.drawable.block_o
        ),
        PieceInfo(
            shape = listOf(listOf(0, 1, 0), listOf(1, 1, 1)), // T
            color = Color.Magenta,
            drawableResId = R.drawable.block_t
        ),
        PieceInfo(
            shape = listOf(listOf(0, 1, 1), listOf(1, 1, 0)), // S
            color = Color.Green,
            drawableResId = R.drawable.block_s
        ),
        PieceInfo(
            shape = listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // Z
            color = Color.Red,
            drawableResId = R.drawable.block_z
        ),
        PieceInfo(
            shape = listOf(listOf(1, 0, 0), listOf(1, 1, 1)), // J
            color = Color.Blue,
            drawableResId = R.drawable.block_j
        ),
        PieceInfo(
            shape = listOf(listOf(0, 0, 1), listOf(1, 1, 1)), // L
            color = Color.White,
            drawableResId = R.drawable.block_l
        )
    )
}