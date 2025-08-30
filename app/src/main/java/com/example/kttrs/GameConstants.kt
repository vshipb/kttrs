package com.example.kttrs

object GameConstants {
    const val BOARD_WIDTH = 10
    const val BOARD_HEIGHT = 22

    

    // SRS Kick Data
    // Based on https://tetris.wiki/Super_Rotation_System

    // Kicks for J, L, S, T, Z pieces
    val commonKickData = mapOf(
        // 0 -> R
        0 to 1 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // R -> 0
        1 to 0 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2)),
        // R -> 2
        1 to 2 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2)),
        // 2 -> R
        2 to 1 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // 2 -> L
        2 to 3 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2)),
        // L -> 2
        3 to 2 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // L -> 0
        3 to 0 to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // 0 -> 3
        0 to 3 to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2))
    )

    // Kicks for I piece
    val iKickData = mapOf(
        // 0 -> R
        0 to 1 to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, -1), Pair(1, 2)),
        // R -> 0
        1 to 0 to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, 1), Pair(-1, -2)),
        // R -> 2
        1 to 2 to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, 2), Pair(2, -1)),
        // 2 -> R
        2 to 1 to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, -2), Pair(-2, 1)),
        // 2 -> L
        2 to 3 to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, 1), Pair(-1, -2)),
        // L -> 2
        3 to 2 to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, -1), Pair(1, 2)),
        // L -> 0
        3 to 0 to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, -2), Pair(-2, 1)),
        // 0 -> L
        0 to 3 to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, 2), Pair(2, -1))
    )
}
