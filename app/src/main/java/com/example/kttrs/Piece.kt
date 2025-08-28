package com.example.kttrs

import androidx.compose.ui.graphics.Color

enum class PieceType {
    I, O, T, S, Z, J, L
}

data class Piece(
    val type: PieceType,
    val shape: List<List<Int>> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
    val rotation: Int = 0,
    val drawableResId: Int = 0
)

data class PieceInfo(
    val type: PieceType,
    val shape: List<List<Int>>,
    val color: Color,
    val drawableResId: Int
)
