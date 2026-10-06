package de.dh.pump.dana

import de.dh.pump.dana.commands.DanaRsPacketDirection
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DanaRsPacketRegistryTest {
    @Test
    fun registryContainsAllNonUnusedReferencePackets() {
        assertEquals(43, DanaRsPacketRegistry.all.size)
    }

    @Test
    fun registryUsesExtractedAapsOpcodes() {
        assertEquals(0x02, DanaRsPacketRegistry.GENERAL_INITIAL_SCREEN_INFORMATION.opcode)
        assertEquals(0xc1, DanaRsPacketRegistry.APS_BASAL_SET_TEMPORARY_BASAL.opcode)
        assertEquals(0x4a, DanaRsPacketRegistry.BOLUS_SET_STEP_BOLUS_START.opcode)
    }

    @Test
    fun notifyPacketsAreAddressableByResponseTypeAndOpcode() {
        val definition = DanaRsPacketRegistry.findByResponse(0xc3, 0x03)

        assertNotNull(definition)
        assertEquals(DanaRsPacketDirection.NOTIFY, definition?.direction)
        assertEquals("NOTIFY__ALARM", definition?.friendlyName)
    }
}