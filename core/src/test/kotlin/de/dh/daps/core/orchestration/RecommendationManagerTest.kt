package de.dh.daps.core.orchestration

import de.dh.daps.common.domain.model.therapy.InsulinAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationManagerTest {

    @Test
    fun testAddBolusRecommendationRoundsAmountToTwoDecimals() {
        val manager = RecommendationManager()

        manager.addBolusRecommendation(
            amount = InsulinAmount(1.234567),
            correctionPart = InsulinAmount(0.5555),
            basalPart = InsulinAmount(0.6789),
        )

        val recommendations = manager.recommendations.value
        assertEquals(1, recommendations.all.size)

        val bolusRec = recommendations.bolus
        assertNotNull(bolusRec)
        assertEquals(1.23, bolusRec!!.amount.iu, 0.0001)
        assertEquals(0.56, bolusRec.correctionPart.iu, 0.0001)
        assertEquals(0.68, bolusRec.basalPart.iu, 0.0001)
    }

    @Test
    fun testBolusRecommendationReplacesPreviousBolus() {
        val manager = RecommendationManager()

        manager.addBolusRecommendation(amount = InsulinAmount(1.0))
        assertEquals(1.0, manager.recommendations.value.bolus?.amount?.iu!!, 0.0001)

        manager.addBolusRecommendation(amount = InsulinAmount(2.5))
        assertEquals(1, manager.recommendations.value.all.size)
        assertEquals(2.5, manager.recommendations.value.bolus?.amount?.iu!!, 0.0001)
    }

    @Test
    fun testMultipleTypesCanBeActiveSimultaneously() {
        val manager = RecommendationManager()

        manager.addCarbsRecommendation(30)
        manager.addBolusRecommendation(amount = InsulinAmount(1.5))
        manager.addTempBasalRecommendation(durationInHours = 1, percent = 120)

        val recommendations = manager.recommendations.value
        assertEquals(3, recommendations.all.size)
        assertEquals(30, recommendations.carbs?.amountInGram)
        assertEquals(1.5, recommendations.bolus?.amount?.iu!!, 0.0001)
        assertEquals(120, recommendations.tempBasal?.percent)
    }

    @Test
    fun testRemoveRecommendation() {
        val manager = RecommendationManager()

        val carbs = ApsRecommendation.Carbs(20)
        manager.addCarbsRecommendation(20)
        assertEquals(1, manager.recommendations.value.all.size)

        manager.removeRecommendation(carbs)
        assertNull(manager.recommendations.value.carbs)
        assertTrue(manager.recommendations.value.isEmpty)
    }

    @Test
    fun testStopClearsRecommendations() {
        val manager = RecommendationManager()

        manager.addCarbsRecommendation(30)
        assertEquals(1, manager.recommendations.value.all.size)

        manager.stop()

        assertEquals(0, manager.recommendations.value.all.size)
        assertTrue(manager.recommendations.value.isEmpty)
    }
}