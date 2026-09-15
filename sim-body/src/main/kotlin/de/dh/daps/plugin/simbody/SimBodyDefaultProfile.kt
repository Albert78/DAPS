package de.dh.daps.plugin.simbody

import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.plugin.simbody.model.BodyProfile

/**
 * A standard insulin type for simulation.
 */
val DEFAULT_SIM_INSULIN_TYPE = InsulinType(
    id = "sim-aspart-id",
    name = "Sim Aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

/**
 * A standard therapy profile.
 */
val DEFAULT_SIM_INSULIN_PROFILE = InsulinProfile(
    name = "Simulator Default",
    basalBlocks = listOf(Block(Minutes.ofHours(24), 0.5)),
    isfBlocks = listOf(Block(Minutes.ofHours(24), 50.0)),
    crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
    insulinType = DEFAULT_SIM_INSULIN_TYPE,
    dia = DEFAULT_SIM_INSULIN_TYPE.dia,
    peak = DEFAULT_SIM_INSULIN_TYPE.peak
)

/**
 * A standard body profile.
 */
val DEFAULT_SIM_BODY_PROFILE = BodyProfile(
    liverGlucoseOutputBlocks = listOf(Block(Minutes.ofHours(24), 5.0)),
    isfBlocks = listOf(Block(Minutes.ofHours(24), 50.0)),
    crBlocks = listOf(Block(Minutes.ofHours(24), 10.0))
)