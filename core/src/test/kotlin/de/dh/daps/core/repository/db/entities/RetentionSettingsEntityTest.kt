package de.dh.daps.core.repository.db.entities

import de.dh.daps.common.domain.model.retention.RetentionOption
import de.dh.daps.common.domain.model.retention.RetentionPolicySettings
import org.junit.Assert.assertEquals
import org.junit.Test

class RetentionSettingsEntityTest {

    @Test
    fun `toModel and toEntity conversions preserve retention option values`() {
        val model = RetentionPolicySettings(
            glucoseReadings = RetentionOption.DAYS_14,
            therapyHistory = RetentionOption.DAYS_180,
            deviceHardwareLogs = RetentionOption.DAYS_14,
            reminders = RetentionOption.DAYS_7,
            systemMetrics = RetentionOption.DAY_1,
            other = RetentionOption.UNLIMITED
        )

        val entity = model.toEntity(id = 42L)
        assertEquals(42L, entity.id)
        assertEquals(RetentionOption.DAYS_14, entity.glucoseReadings)
        assertEquals(RetentionOption.DAYS_180, entity.therapyHistory)
        assertEquals(RetentionOption.DAYS_14, entity.deviceHardwareLogs)
        assertEquals(RetentionOption.DAYS_7, entity.reminders)
        assertEquals(RetentionOption.DAY_1, entity.systemMetrics)
        assertEquals(RetentionOption.UNLIMITED, entity.other)

        val convertedBackModel = entity.toModel()
        assertEquals(model, convertedBackModel)
    }
}