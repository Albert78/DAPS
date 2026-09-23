package de.dh.daps.core.alarms

import de.dh.daps.common.model.data.AlarmType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlarmSnoozeManagerTest {

    private lateinit var snoozeManager: AlarmSnoozeManager

    @Before
    fun setUp() {
        snoozeManager = AlarmSnoozeManager()
    }

    @Test
    fun `snoozing CRITICAL_LOW_BG automatically snoozes LOW_BG`() {
        snoozeManager.snoozeAlarm(AlarmType.CRITICAL_LOW_BG, 15)

        assertTrue(snoozeManager.isSnoozed(AlarmType.CRITICAL_LOW_BG))
        assertTrue(snoozeManager.isSnoozed(AlarmType.LOW_BG))
    }

    @Test
    fun `snoozing LOW_BG does NOT snooze CRITICAL_LOW_BG`() {
        snoozeManager.snoozeAlarm(AlarmType.LOW_BG, 15)

        assertTrue(snoozeManager.isSnoozed(AlarmType.LOW_BG))
        assertFalse(snoozeManager.isSnoozed(AlarmType.CRITICAL_LOW_BG))
    }

    @Test
    fun `clearing CRITICAL_LOW_BG snooze also clears LOW_BG snooze`() {
        snoozeManager.snoozeAlarm(AlarmType.CRITICAL_LOW_BG, 15)
        snoozeManager.clearSnooze(AlarmType.CRITICAL_LOW_BG)

        assertFalse(snoozeManager.isSnoozed(AlarmType.CRITICAL_LOW_BG))
        assertFalse(snoozeManager.isSnoozed(AlarmType.LOW_BG))
    }

    @Test
    fun `clearInactiveAndExpiredSnoozes removes snoozes for inactive alarms`() {
        snoozeManager.snoozeAlarm(AlarmType.LOW_BG, 15)
        assertTrue(snoozeManager.isSnoozed(AlarmType.LOW_BG))

        // Alarm condition cleared (active alarms set is now empty)
        snoozeManager.clearInactiveAndExpiredSnoozes(emptySet())

        assertFalse(snoozeManager.isSnoozed(AlarmType.LOW_BG))
        assertTrue(snoozeManager.snoozedAlarms.value.isEmpty())
    }

    @Test
    fun `clearInactiveAndExpiredSnoozes retains snoozes for active alarms`() {
        snoozeManager.snoozeAlarm(AlarmType.LOW_BG, 15)
        snoozeManager.snoozeAlarm(AlarmType.HIGH_BG, 15)

        // HIGH_BG condition cleared, LOW_BG still active
        snoozeManager.clearInactiveAndExpiredSnoozes(setOf(AlarmType.LOW_BG))

        assertTrue(snoozeManager.isSnoozed(AlarmType.LOW_BG))
        assertFalse(snoozeManager.isSnoozed(AlarmType.HIGH_BG))
    }
}