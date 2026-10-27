package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.domain.model.engine.CurrentSettings
import de.dh.daps.core.repository.db.entities.CurrentSettingsEntity

// Settings Converters
fun CurrentSettings.toEntity() = CurrentSettingsEntity(
    id = this.id,
    aps_mode = this.apsMode
)

fun CurrentSettingsEntity.toModel() = CurrentSettings(
    id = this.id,
    apsMode = this.aps_mode
)