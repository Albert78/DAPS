package de.dh.pump.dana.commands

enum class DanaRsBolusSpeed(val wireValue: Int) {
    U12_SECONDS(0),
    U30_SECONDS(1),
    U60_SECONDS(2);

    companion object {
        fun fromWireValue(value: Int): DanaRsBolusSpeed = entries.firstOrNull { it.wireValue == value } ?: U12_SECONDS
    }
}