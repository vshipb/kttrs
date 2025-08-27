package com.example.kttrs

import androidx.compose.ui.graphics.Color

data class Piece(
    val shape: List<List<Int>> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
    val drawableResId: Int = 0
)

data class PieceInfo(
    val shape: List<List<Int>>,
    val color: Color,
    val drawableResId: Int
)