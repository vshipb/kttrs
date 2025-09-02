package com.example.kttrs

object SevenBagRandomizer {
    private var bag = mutableListOf<PieceType>()

    init {
        fillBag()
    }

    private fun fillBag() {
        bag.clear()
        val allPieceTypes = PieceType.entries.toMutableList()
        allPieceTypes.shuffle()
        bag.addAll(allPieceTypes)
    }

    fun nextPiece(): PieceType {
        if (bag.isEmpty()) {
            fillBag()
        }
        return bag.removeAt(0)
    }

    fun restart() {
        fillBag()
    }
}
