package com.example.kttrs

interface PieceSpec {
    val shape: List<List<Int>>
    val drawableResId: Int
    val type: PieceType?
}

enum class PieceType(
    private val baseShape: List<List<Int>>,
    override val drawableResId: Int
) : PieceSpec {
    I(listOf(listOf(1, 1, 1, 1)), R.drawable.block_i),
    O(listOf(listOf(1, 1), listOf(1, 1)), R.drawable.block_o),
    T(listOf(listOf(0, 1, 0), listOf(1, 1, 1)), R.drawable.block_t),
    S(listOf(listOf(0, 1, 1), listOf(1, 1, 0)), R.drawable.block_s),
    Z(listOf(listOf(1, 1, 0), listOf(0, 1, 1)), R.drawable.block_z),
    J(listOf(listOf(1, 0, 0), listOf(1, 1, 1)), R.drawable.block_j),
    L(listOf(listOf(0, 0, 1), listOf(1, 1, 1)), R.drawable.block_l);

    override val shape: List<List<Int>>
        get() = baseShape

    override val type: PieceType
        get() = this
}

data class Piece(
    val spec: PieceSpec,
    val x: Int = 0,
    val y: Int = 0,
    val rotation: Int = 0
)

val Piece.shape: List<List<Int>>
    get() {
        var currentShape = this.spec.shape
        repeat(this.rotation) {
            currentShape = List(currentShape[0].size) { y ->
                List(currentShape.size) { x ->
                    currentShape[currentShape.size - 1 - x][y]
                }
            }
        }
        return currentShape
    }

val Piece.drawableResId: Int
    get() = this.spec.drawableResId

val Piece.type: PieceType?
    get() = this.spec.type
