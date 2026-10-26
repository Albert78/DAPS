package de.dh.daps.core.orchestration

import de.dh.daps.common.model.InsulinAmount
import org.junit.Assert.assertEquals
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
        assertEquals(1, recommendations.size)

        val bolusRec = recommendations.first() as ApsRecommendation.Bolus
        assertEquals(1.23, bolusRec.amount.iu, 0.0001)
        assertEquals(0.56, bolusRec.correctionPart.iu, 0.0001)
        assertEquals(0.68, bolusRec.basalPart.iu, 0.0001)
    }

    @Test
    fun testStopClearsRecommendations() {
        val manager = RecommendationManager()

        manager.addCarbsRecommendation(30)
        assertEquals(1, manager.recommendations.value.size)

        manager.stop()

        assertEquals(0, manager.recommendations.value.size)
    }
}