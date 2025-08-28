package com.example.kttrs

enum class PieceType(
    val shape: List<List<Int>>,
    val drawableResId: Int
) {
    I(listOf(listOf(1, 1, 1, 1)), R.drawable.block_i),
    O(listOf(listOf(1, 1), listOf(1, 1)), R.drawable.block_o),
    T(listOf(listOf(0, 1, 0), listOf(1, 1, 1)), R.drawable.block_t),
    S(listOf(listOf(0, 1, 1), listOf(1, 1, 0)), R.drawable.block_s),
    Z(listOf(listOf(1, 1, 0), listOf(0, 1, 1)), R.drawable.block_z),
    J(listOf(listOf(1, 0, 0), listOf(1, 1, 1)), R.drawable.block_j),
    L(listOf(listOf(0, 0, 1), listOf(1, 1, 1)), R.drawable.block_l)
}

data class Piece(
    val type: PieceType,
    val shape: List<List<Int>> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
    val rotation: Int = 0,
    val drawableResId: Int = 0
)
