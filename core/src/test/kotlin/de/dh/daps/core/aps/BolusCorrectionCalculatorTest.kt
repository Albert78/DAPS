package de.dh.daps.core.aps

import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BolusCorrectionCalculatorTest {

    private val targetBg = BgValue.fromMgDl(100.0)
    private val lowThreshold = BgValue.fromMgDl(70.0)

    @Test
    fun testSuggestedImiElevatedBg() {
        val currentBg = BgValue.fromMgDl(180.0)
        val imi = BolusCalculationMath.calculateSuggestedImi(
            currentBg = currentBg,
            targetBg = targetBg,
            lowThreshold = lowThreshold
        )
        // (180 - 100) / 20 * 5 = 20 min
        assertEquals(Minutes(20).value, imi?.value)
    }

    @Test
    fun testSuggestedImiSuppressedByRescueCarbs() {
        val currentBg = BgValue.fromMgDl(180.0)
        val imi = BolusCalculationMath.calculateSuggestedImi(
            currentBg = currentBg,
            targetBg = targetBg,
            lowThreshold = lowThreshold,
            isRescueCarbs = true
        )
        assertEquals(Minutes(0).value, imi?.value)
    }

    @Test
    fun testSuggestedImiSuppressedByImpendingLow() {
        val currentBg = BgValue.fromMgDl(120.0)
        val impendingLow = ProjectedBg(timestamp = Timestamp.now(), bg = BgValue.fromMgDl(65.0))
        val imi = BolusCalculationMath.calculateSuggestedImi(
            currentBg = currentBg,
            targetBg = targetBg,
            lowThreshold = lowThreshold,
            impendingLow = impendingLow
        )
        assertEquals(Minutes(0).value, imi?.value)
    }

    @Test
    fun testSuggestedImiSuppressedByLowBg() {
        val currentBg = BgValue.fromMgDl(60.0)
        val imi = BolusCalculationMath.calculateSuggestedImi(
            currentBg = currentBg,
            targetBg = targetBg,
            lowThreshold = lowThreshold
        )
        assertEquals(Minutes(0).value, imi?.value)
    }

    @Test
    fun testBolusPartsNormalMeal() {
        val cr = 10.0 // 1 U per 10g
        val isf = BgDelta.fromMgDl(40)

        val parts = BolusCalculationMath.calculateBolusParts(
            carbsGrams = 30.0,
            mealTimestamp = Timestamp.now(),
            bg = targetBg,
            cr = cr,
            isf = isf,
            targetBg = targetBg,
            impendingLow = null,
            netIob = InsulinAmount.ZERO,
            cob = 0.0,
            futureCarbs = 0.0,
            deferredBolusAmount = InsulinAmount.ZERO
        )

        assertEquals(3.0, parts.mealPart.iu, 0.001)
        assertEquals(3.0, parts.totalProposed.iu, 0.001)
    }

    @Test
    fun testBolusPartsRescueCarbsExemptedFromMealDose() {
        val cr = 10.0 // 1 U per 10g
        val isf = BgDelta.fromMgDl(40)

        val parts = BolusCalculationMath.calculateBolusParts(
            carbsGrams = 10.0,
            mealTimestamp = Timestamp.now(),
            bg = targetBg,
            cr = cr,
            isf = isf,
            targetBg = targetBg,
            impendingLow = null,
            netIob = InsulinAmount.ZERO,
            cob = 0.0,
            futureCarbs = 0.0,
            deferredBolusAmount = InsulinAmount.ZERO,
            isRescueCarbs = true,
            rescueCarbsAmount = 10.0
        )

        // 10g rescue carbs should not produce a meal insulin requirement
        assertEquals(0.0, parts.mealPart.iu, 0.001)
        assertEquals(0.0, parts.totalProposed.iu, 0.001)
    }

    @Test
    fun testBolusPartsPartialRescueCarbs() {
        val cr = 10.0 // 1 U per 10g
        val isf = BgDelta.fromMgDl(40)

        val parts = BolusCalculationMath.calculateBolusParts(
            carbsGrams = 25.0,
            mealTimestamp = Timestamp.now(),
            bg = targetBg,
            cr = cr,
            isf = isf,
            targetBg = targetBg,
            impendingLow = null,
            netIob = InsulinAmount.ZERO,
            cob = 0.0,
            futureCarbs = 0.0,
            deferredBolusAmount = InsulinAmount.ZERO,
            isRescueCarbs = true,
            rescueCarbsAmount = 10.0
        )

        // Net carbs for insulin = 25 - 10 = 15g -> 1.5 U
        assertEquals(1.5, parts.mealPart.iu, 0.001)
        assertEquals(1.5, parts.totalProposed.iu, 0.001)
    }

    @Test
    fun testMealBolusContextPrimitiveRescueCarbs() {
        val context = MealBolusContext(
            rescueCarbsGrams = 15.0,
            recommendedBolusAmount = InsulinAmount(1.5)
        )
        assertTrue(context.isRescueCarbs)
        assertEquals(15.0, context.rescueCarbsGrams, 0.001)
        assertEquals(1.5, context.recommendedBolusAmount?.iu!!, 0.001)
    }
}