package de.dh.daps.plugin.glucose.receiver

import androidx.annotation.StringRes
import de.dh.daps.common.domain.model.therapy.BgReadingsInterval
import de.dh.daps.common.domain.model.therapy.Minutes

/**
 * Enum which determines a type of external glucose source.
 */
enum class ExternalSourceType(
    val readingsInterval: BgReadingsInterval,
    val readingsTimeDelay: Minutes,
    @StringRes val displayNameRes: Int,
) {
    xDrip1Min(
        readingsInterval = BgReadingsInterval.OneMinute,
        readingsTimeDelay = Minutes(1),
        displayNameRes = R.string.source_type_xdrip_1min,
    ),
    xDrip5Min(
        readingsInterval = BgReadingsInterval.FiveMinutes,
        readingsTimeDelay = Minutes(5),
        displayNameRes = R.string.source_type_xdrip_5min,
    ),
}