package de.dh.daps.plugin.simbody.model

import de.dh.daps.common.model.data.Block

/**
 * Metabolic profile of a body for simulation.
 */
data class BodyProfile(
    val crBlocks: List<Block>,
    val isfBlocks: List<Block>,
    val liverGlucoseOutputBlocks: List<Block>
)