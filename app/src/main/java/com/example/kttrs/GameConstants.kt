package com.example.kttrs

import androidx.compose.ui.graphics.Color

object GameConstants {
    const val BOARD_WIDTH = 10
    const val BOARD_HEIGHT = 22

    val pieceInfos = listOf(
        PieceInfo(
            type = PieceType.I,
            shape = listOf(listOf(1, 1, 1, 1)), // I
            color = Color.Cyan,
            drawableResId = R.drawable.block_i
        ),
        PieceInfo(
            type = PieceType.O,
            shape = listOf(listOf(1, 1), listOf(1, 1)), // O
            color = Color.Yellow,
            drawableResId = R.drawable.block_o
        ),
        PieceInfo(
            type = PieceType.T,
            shape = listOf(listOf(0, 1, 0), listOf(1, 1, 1)), // T
            color = Color.Magenta,
            drawableResId = R.drawable.block_t
        ),
        PieceInfo(
            type = PieceType.S,
            shape = listOf(listOf(0, 1, 1), listOf(1, 1, 0)), // S
            color = Color.Green,
            drawableResId = R.drawable.block_s
        ),
        PieceInfo(
            type = PieceType.Z,
            shape = listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // Z
            color = Color.Red,
            drawableResId = R.drawable.block_z
        ),
        PieceInfo(
            type = PieceType.J,
            shape = listOf(listOf(1, 0, 0), listOf(1, 1, 1)), // J
            color = Color.Blue,
            drawableResId = R.drawable.block_j
        ),
        PieceInfo(
            type = PieceType.L,
            shape = listOf(listOf(0, 0, 1), listOf(1, 1, 1)), // L
            color = Color.White,
            drawableResId = R.drawable.block_l
        )
    )

    // SRS Kick Data
    // Based on https://tetris.wiki/Super_Rotation_System

    // Kicks for J, L, S, T, Z pieces
    val commonKickData = mapOf(
        // 0 -> 1
        0 to 1 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // 1 -> 0
        1 to 0 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2)),
        // 1 -> 2
        1 to 2 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2)),
        // 2 -> 1
        2 to 1 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // 2 -> 3
        2 to 3 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2)),
        // 3 -> 2
        3 to 2 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // 3 -> 0
        3 to 0 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // 0 -> 3
        0 to 3 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2))
    )

    // Kicks for I piece
    val iKickData = mapOf(
        // 0 -> 1
        0 to 1 to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, 1), Pair(1, -2)),
        // 1 -> 0
        1 to 0 to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, -1), Pair(-1, 2)),
        // 1 -> 2
        1 to 2 to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, -2), Pair(2, 1)),
        // 2 -> 1
        2 to 1 to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, 2), Pair(-2, -1)),
        // 2 -> 3
        2 to 3 to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, -1), Pair(-1, 2)),
        // 3 -> 2
        3 to 2 to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, 1), Pair(1, -2)),
        // 3 -> 0
        3 to 0 to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, 2), Pair(-2, -1)),
        // 0 -> 3
        0 to 3 to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, -2), Pair(2, 1))
    )
}
