package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType

/**
 * A therapy profile that defines a set of therapy factors.
 * Profiles are used to switch between different metabolic states (e.g. Normal, Sport, Illness).
 */
data class InsulinProfile(
    var id: Long = ID_UNDEFINED,
    val name: String,
    val basalBlocks: List<Block>,
    val isfBlocks: List<Block>,
    val crBlocks: List<Block>,
    val insulinType: InsulinType,
    val insulinConcentration: InsulinConcentration = InsulinConcentration.U100,
    val dia: Minutes,
    val peak: Minutes
)