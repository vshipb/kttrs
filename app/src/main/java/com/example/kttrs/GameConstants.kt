package com.example.kttrs

import androidx.compose.ui.graphics.Color

object GameConstants {
    const val BOARD_WIDTH = 10
    const val BOARD_HEIGHT = 20

    val shapes = listOf(
        listOf(listOf(1, 1, 1, 1)), // I
        listOf(listOf(1, 1), listOf(1, 1)), // O
        listOf(listOf(0, 1, 0), listOf(1, 1, 1)), // T
        listOf(listOf(0, 1, 1), listOf(1, 1, 0)), // S
        listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // Z
        listOf(listOf(1, 0, 0), listOf(1, 1, 1)), // J
        listOf(listOf(0, 0, 1), listOf(1, 1, 1))  // L
    )

    val colors = listOf(
        Color.Cyan,
        Color.Yellow,
        Color.Magenta,
        Color.Green,
        Color.Red,
        Color.Blue,
        Color.White
    )
}
