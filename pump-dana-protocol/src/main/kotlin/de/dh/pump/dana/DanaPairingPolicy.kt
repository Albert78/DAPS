package de.dh.pump.dana

/**
 * Documents pairing behavior that differs between Dana pump generations.
 *
 * Pairing policy is kept separate from packet encoding because Android bonding, pump-side approval,
 * and app-level pairing codes are UX/session concerns rather than command payload details.
 */
data class DanaPairingPolicy(
    val model: DanaPumpModel,
    val usesAndroidBondingDialog: Boolean,
    val requiresPumpSideConfirmation: Boolean,
    val requiresAppPairingCodes: Boolean,
) {
    companion object {
        val DANA_I = DanaPairingPolicy(
            model = DanaPumpModel.DANA_I,
            usesAndroidBondingDialog = true,
            requiresPumpSideConfirmation = true,
            requiresAppPairingCodes = false,
        )

        val DANA_RS_V3 = DanaPairingPolicy(
            model = DanaPumpModel.DANA_RS_V3,
            usesAndroidBondingDialog = false,
            requiresPumpSideConfirmation = true,
            requiresAppPairingCodes = true,
        )
    }
}