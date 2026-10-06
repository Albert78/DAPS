package de.dh.pump.dana.commands

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.general.GeneralInitialScreenInformationCommand
import de.dh.pump.protocol.ByteReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests parsing of real-world Dana packets.
 *
 * This demonstrates how the generic [de.dh.pump.commands.PumpCommand] interface 
 * is used to implement Dana-specific business logic.
 */
class GeneralCommandParsingTest {

    @Test
    fun parsesInitialScreenInformation() {
        val command = GeneralInitialScreenInformationCommand()
        
        // Example hex from a Dana pump response (without protocol envelope)
        // [Status (0x01 = Suspended), Total (1.23 U), Max (100 U), Reservoir (150.5 U), 
        //  Basal (0.05 U/h), Temp (0%), Battery (95%), Ext (0.0 U/h), IOB (0.5 U), Error (None)]
        val payload = byteArrayOf(
            0x01,                   // Status: Suspended
            0x7B, 0x00,             // Daily Total: 123 centi-units = 1.23 U
            0x10, 0x27,             // Max Daily: 10000 centi-units = 100 U
            0x1A, 0x3A,             // Reservoir: 15050 centi-units = 150.5 U
            0x05, 0x00,             // Basal: 5 centi-units/h = 0.05 U/h
            0x00,                   // Temp Basal: 0%
            0x5F,                   // Battery: 95%
            0x00, 0x00,             // Extended Bolus Rate: 0.0
            0x32, 0x00,             // IOB: 50 centi-units = 0.5 U
            0x00                    // Error: None
        )
        
        val response = command.decodePayload(ByteReader(payload))
        
        assertEquals(PumpStatus.OK, response.status)
        assertTrue(response.pumpSuspended)
        assertEquals(1.23, response.dailyTotalUnits, 0.001)
        assertEquals(150.5, response.reservoirRemainingUnits, 0.001)
        assertEquals(0.05, response.currentBasalUnitsPerHour, 0.001)
        assertEquals(95, response.batteryRemainingPercent)
        assertEquals(0.5, response.insulinOnBoardUnits, 0.001)
    }
}