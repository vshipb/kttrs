package vsh.kttrs.model

object ThirtyFiveBagRandomizer {
    private var bag = mutableListOf<PieceType>()

    init {
        fillBag()
    }

    private fun fillBag() {
        bag.clear()
        val allPieceTypes = mutableListOf<PieceType>()
        repeat(5) {
            allPieceTypes.addAll(PieceType.entries)
        }
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