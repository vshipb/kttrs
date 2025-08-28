package com.example.kttrs

data class TestPieceSpec(
    override val shape: List<List<Int>>,
    override val drawableResId: Int = 0,
    override val type: PieceType? = null
) : PieceSpec
