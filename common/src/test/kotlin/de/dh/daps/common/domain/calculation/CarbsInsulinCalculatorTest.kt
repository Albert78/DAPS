package de.dh.daps.common.domain.calculation

import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinDose
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.ProfileInsulinSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarbsInsulinCalculatorTest {
    @Test
    fun testIobCalculatesPerDoseInsulinType() {
        val calculator = CarbsInsulinCalculator(Minutes(5))

        val typeA = InsulinType(id = "type-a", name = "Rapid A", activeSubstance = "Substance A", dia = Minutes(300), peak = Minutes(75))
        val typeB = InsulinType(id = "type-b", name = "Rapid B", activeSubstance = "Substance B", dia = Minutes(240), peak = Minutes(55))

        val profile = InsulinProfile(
            name = "Test Profile",
            basalBlocks = emptyList(),
            isfBlocks = emptyList(),
            crBlocks = emptyList(),
            insulinSettings = listOf(
                ProfileInsulinSetting(insulinType = typeA, dia = Minutes(300), peak = Minutes(75)),
                ProfileInsulinSetting(insulinType = typeB, dia = Minutes(180), peak = Minutes(45))
            )
        )

        val now = Timestamp.now()
        val doseA = InsulinDose(
            timestamp = now.minusMinutes(90),
            amount = InsulinAmount(10.0),
            insulinType = typeA
        )
        val doseB = InsulinDose(
            timestamp = now.minusMinutes(90),
            amount = InsulinAmount(10.0),
            insulinType = typeB
        )

        val totalIob = calculator.iob(
            insulinDoses = listOf(doseA, doseB),
            timestamp = now,
            profile = profile
        )

        val iobA = calculator.iob(
            insulinDoses = listOf(doseA),
            timestamp = now,
            dia = Minutes(300),
            peak = Minutes(75)
        )

        val iobB = calculator.iob(
            insulinDoses = listOf(doseB),
            timestamp = now,
            dia = Minutes(180),
            peak = Minutes(45)
        )

        assertEquals(iobA.iu + iobB.iu, totalIob.iu, 0.0001)
        assertTrue(totalIob.iu > 0)
    }

    @Test
    fun testFallbackToTypeDefaultsWhenNotInProfile() {
        val calculator = CarbsInsulinCalculator(Minutes(5))

        val typeCustom = InsulinType(id = "type-custom", name = "Custom", activeSubstance = "Substance Custom", dia = Minutes(250), peak = Minutes(60))
        val profile = InsulinProfile(
            name = "Empty Settings Profile",
            basalBlocks = emptyList(),
            isfBlocks = emptyList(),
            crBlocks = emptyList(),
            insulinSettings = emptyList()
        )

        val now = Timestamp.now()
        val dose = InsulinDose(
            timestamp = now.minusMinutes(60),
            amount = InsulinAmount(5.0),
            insulinType = typeCustom
        )

        val iobProfile = calculator.iob(
            insulinDoses = listOf(dose),
            timestamp = now,
            profile = profile
        )

        val iobExplicit = calculator.iob(
            insulinDoses = listOf(dose),
            timestamp = now,
            dia = typeCustom.dia,
            peak = typeCustom.peak
        )

        assertEquals(iobExplicit.iu, iobProfile.iu, 0.0001)
    }
}