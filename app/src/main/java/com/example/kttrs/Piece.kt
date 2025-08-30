package com.example.kttrs

interface PieceSpec {
    fun getShapeForRotation(rotationIndex: Int): List<List<Int>>
    val drawableResId: Int
    val type: PieceType?
}

enum class PieceType(
    private val allRotationShapes: List<List<List<Int>>>,
    override val drawableResId: Int
) : PieceSpec {
    // SRS shape definitions.
    // Each matrix represents the piece's bounding box.
    // 0: Spawn state
    // 1: Rotated 90 degrees clockwise from spawn
    // 2: Rotated 180 degrees from spawn
    // 3: Rotated 270 degrees clockwise (or 90 counter-clockwise) from spawn
    I(
        listOf(
            listOf(listOf(0,0,0,0), listOf(1,1,1,1), listOf(0,0,0,0), listOf(0,0,0,0)),
            listOf(listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0)),
            listOf(listOf(0,0,0,0), listOf(0,0,0,0), listOf(1,1,1,1), listOf(0,0,0,0)),
            listOf(listOf(0,1,0,0), listOf(0,1,0,0), listOf(0,1,0,0), listOf(0,1,0,0))
        ),
        R.drawable.sblock_i
    ),
    O(
        listOf(
            // O-shape is the same in all rotations
            listOf(listOf(1,1), listOf(1,1)),
            listOf(listOf(1,1), listOf(1,1)),
            listOf(listOf(1,1), listOf(1,1)),
            listOf(listOf(1,1), listOf(1,1))
        ),
        R.drawable.sblock_o
    ),
    T(
        listOf(
            listOf(listOf(0,1,0), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,1), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(0,1,0)),
            listOf(listOf(0,1,0), listOf(1,1,0), listOf(0,1,0))
        ),
        R.drawable.sblock_t
    ),
    S(
        listOf(
            listOf(listOf(0,1,1), listOf(1,1,0), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,1), listOf(0,0,1)),
            listOf(listOf(0,0,0), listOf(0,1,1), listOf(1,1,0)),
            listOf(listOf(1,0,0), listOf(1,1,0), listOf(0,1,0))
        ),
        R.drawable.sblock_s
    ),
    Z(
        listOf(
            listOf(listOf(1,1,0), listOf(0,1,1), listOf(0,0,0)),
            listOf(listOf(0,0,1), listOf(0,1,1), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,0), listOf(0,1,1)),
            listOf(listOf(0,1,0), listOf(1,1,0), listOf(1,0,0))
        ),
        R.drawable.sblock_z
    ),
    J(
        listOf(
            listOf(listOf(1,0,0), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,1), listOf(0,1,0), listOf(0,1,0)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(0,0,1)),
            listOf(listOf(0,1,0), listOf(0,1,0), listOf(1,1,0))
        ),
        R.drawable.sblock_j
    ),
    L(
        listOf(
            listOf(listOf(0,0,1), listOf(1,1,1), listOf(0,0,0)),
            listOf(listOf(0,1,0), listOf(0,1,0), listOf(0,1,1)),
            listOf(listOf(0,0,0), listOf(1,1,1), listOf(1,0,0)),
            listOf(listOf(1,1,0), listOf(0,1,0), listOf(0,1,0))
        ),
        R.drawable.sblock_l
    );

    override fun getShapeForRotation(rotationIndex: Int): List<List<Int>> {
        return allRotationShapes[rotationIndex % allRotationShapes.size]
    }

    override val type: PieceType
        get() = this
}

data class Piece(
    val spec: PieceSpec,
    val x: Int = 0, // X-coordinate of the piece's anchor point on the board
    val y: Int = 0, // Y-coordinate of the piece's anchor point on the board
    val rotation: Int = 0 // Current rotation state: 0, 1, 2, or 3
)

val Piece.shape: List<List<Int>>
    get() {
        return this.spec.getShapeForRotation(this.rotation)
    }

val Piece.drawableResId: Int
    get() = this.spec.drawableResId

val Piece.type: PieceType?
    get() = this.spec.type

