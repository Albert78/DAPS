package de.dh.pump.danai.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DanaIPumpSuspendTest {

    @Test
    fun `setSuspend invokes onSetSuspend callback`() = runTest {
        val pump = DanaIPump()
        var lastSuspendValue: Boolean? = null

        pump.onSetSuspend = { suspended ->
            lastSuspendValue = suspended
        }

        pump.setSuspend(true)
        assertEquals(true, lastSuspendValue)

        pump.setSuspend(false)
        assertEquals(false, lastSuspendValue)
    }
}