package vsh.kttrs

import vsh.kttrs.model.PieceSpec
import vsh.kttrs.model.PieceType

data class TestPieceSpec(
    val shape: List<List<Int>>, // 'override' removed as 'shape' is not in PieceSpec interface
    override val drawableResId: Int = 0,
    override val type: PieceType? = null
) : PieceSpec {
    override fun getShapeForRotation(rotationIndex: Int): List<List<Int>> {
        // For testing purposes, assume the provided 'shape' is used for all rotations,
        // or that this test piece does not rotate.
        return shape
    }
}
