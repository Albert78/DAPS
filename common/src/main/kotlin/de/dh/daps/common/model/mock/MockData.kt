package de.dh.daps.common.model.mock

import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.ProfileInsulinSetting

fun mockInsulinType() = InsulinType(
    name = "Mock Insulin",
    activeSubstance = "Insulin aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

fun mockSimpleInsulinProfile() =
    InsulinProfile(
        name = "Mock Profile",
        crBlocks = listOf(
            Block(
                Minutes.ONE_DAY,
                10.0
            )
        ),
        isfBlocks = listOf(
            Block(
                Minutes.ONE_DAY,
                44.0
            )
        ),
        basalBlocks = listOf(
            Block(
                Minutes.ONE_DAY,
                0.5
            )
        ),
        insulinSettings = listOf(
            ProfileInsulinSetting(
                insulinType = mockInsulinType(),
                dia = Minutes.ofHours(5),
                peak = Minutes(75)
            )
        )
    )