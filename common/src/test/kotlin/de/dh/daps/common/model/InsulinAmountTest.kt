package de.dh.daps.common.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InsulinAmountTest {

    @Test
    fun testRoundToTwoDecimals() {
        val amount1 = InsulinAmount(1.23456)
        assertEquals(1.23, amount1.roundToTwoDecimals().iu, 0.0001)

        val amount2 = InsulinAmount(1.23556)
        assertEquals(1.24, amount2.roundToTwoDecimals().iu, 0.0001)

        val amount3 = InsulinAmount(2.5)
        assertEquals(2.50, amount3.roundToTwoDecimals().iu, 0.0001)

        val amount4 = InsulinAmount(0.123)
        assertEquals(0.12, amount4.roundToTwoDecimals().iu, 0.0001)
    }
}