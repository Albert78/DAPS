package de.dh.pump.danai.core.model

/**
 * Persistable state of a Dana-i pump.
 */
data class DanaPumpState(
    val name: String = "",
    val address: String = "",
    val ble5PairingKey: String? = null,
    val hardwareModel: Int? = null,
    val protocol: Int? = null,
    val activeBolusId: String? = null,
    val activeBolusTargetAmount: Double? = null,
    val activeBolusStartTime: Long? = null,
)