package de.dh.daps.common.domain.model.retention

import de.dh.daps.common.domain.model.data.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetentionPolicySettingsTest {

    @Test
    fun `default retention options match expected system defaults`() {
        val settings = RetentionPolicySettings()
        assertEquals(RetentionOption.DAYS_30, settings.glucoseReadings)
        assertEquals(RetentionOption.DAYS_90, settings.therapyHistory)
        assertEquals(RetentionOption.DAYS_7, settings.deviceHardwareLogs)
        assertEquals(RetentionOption.DAYS_3, settings.reminders)
        assertEquals(RetentionOption.DAYS_3, settings.systemMetrics)
        assertEquals(RetentionOption.DAYS_30, settings.other)
    }

    @Test
    fun `calculateCutoff returns correct timestamp subtraction`() {
        val now = Timestamp(1_000_000_000_000L)
        val cutoff3Days = RetentionOption.DAYS_3.calculateCutoff(now)
        val expected3DaysMs = 1_000_000_000_000L - (3 * 24 * 60 * 60 * 1000L)
        assertEquals(expected3DaysMs, cutoff3Days?.ms)
    }

    @Test
    fun `calculateCutoff returns null for unlimited option`() {
        val now = Timestamp(1_000_000_000_000L)
        assertNull(RetentionOption.UNLIMITED.calculateCutoff(now))
    }

    @Test
    fun `computeCutoffs returns map with all categories`() {
        val now = Timestamp(1_000_000_000_000L)
        val settings = RetentionPolicySettings(
            reminders = RetentionOption.DAY_1,
            other = RetentionOption.UNLIMITED
        )
        val cutoffs = settings.computeCutoffs(now)

        assertEquals(DataRetentionCategory.entries.size, cutoffs.size)
        assertEquals(now.minusHours(24).ms, cutoffs[DataRetentionCategory.REMINDERS]?.ms)
        assertNull(cutoffs[DataRetentionCategory.OTHER])
    }
}