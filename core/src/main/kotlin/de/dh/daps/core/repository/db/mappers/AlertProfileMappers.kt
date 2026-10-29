package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.alarm.AlertDisplayMode
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.RecommendationType
import de.dh.daps.common.domain.model.alarm.SoundConfig
import de.dh.daps.common.domain.model.alarm.VibrationMode
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.core.repository.db.entities.AlertProfileEntity
import org.json.JSONObject

fun AlertProfileEntity.toModel(): AlertProfile {
    return AlertProfile(
        id = id,
        name = name,
        isDefault = isDefault,
        alarmDefaults = parseAlarmDefaults(alarmDefaultsJson),
        alarmOverrides = parseAlarmOverrides(alarmOverridesJson),
        recommendationDefault = parseAlertSignalConfig(recommendationDefaultJson),
        recommendationOverrides = parseRecommendationOverrides(recommendationOverridesJson),
        reminderDefault = parseAlertSignalConfig(reminderDefaultJson),
        reminderOverrides = parseReminderOverrides(reminderOverridesJson)
    )
}

fun AlertProfile.toEntity(isActive: Boolean = false): AlertProfileEntity {
    return AlertProfileEntity(
        id = id,
        name = name,
        isDefault = isDefault,
        isActive = isActive,
        alarmDefaultsJson = alarmDefaultsToJson(alarmDefaults),
        alarmOverridesJson = alarmOverridesToJson(alarmOverrides),
        recommendationDefaultJson = recommendationDefault.toJson().toString(),
        recommendationOverridesJson = recommendationOverridesToJson(recommendationOverrides),
        reminderDefaultJson = reminderDefault.toJson().toString(),
        reminderOverridesJson = reminderOverridesToJson(reminderOverrides)
    )
}

private fun AlertSignalConfig.toJson(): JSONObject {
    val json = JSONObject()
    json.put("enabled", enabled)
    json.put("isFullScreen", isFullScreen)
    val sound = soundConfig
    json.put("hasSound", sound != null)
    if (sound != null) {
        json.put("volume", sound.volume)
        json.put("soundUri", sound.soundUri ?: JSONObject.NULL)
    }
    json.put("vibrationMode", vibrationMode.name)
    json.put("overrideDnd", overrideDnd)
    return json
}

private fun parseAlertSignalConfig(jsonStr: String?): AlertSignalConfig {
    if (jsonStr.isNullOrEmpty()) return AlertSignalConfig()
    return try {
        val json = JSONObject(jsonStr)
        json.toAlertSignalConfig()
    } catch (_: Exception) {
        AlertSignalConfig()
    }
}

private fun JSONObject.toAlertSignalConfig(): AlertSignalConfig {
    val enabled = optBoolean("enabled", true)
    val isFS = optBoolean("isFullScreen", false)
    val hasSound = if (has("hasSound")) optBoolean("hasSound") else has("volume")
    val displayMode = if (isFS) {
        if (hasSound) {
            val volume = optInt("volume", 80)
            val soundUri = if (isNull("soundUri") || !has("soundUri")) null else optString("soundUri")
            AlertDisplayMode.FullScreen(sound = SoundConfig(volume = volume, soundUri = soundUri))
        } else {
            AlertDisplayMode.FullScreen(sound = null)
        }
    } else {
        AlertDisplayMode.NotificationOnly
    }

    val vibModeStr = optString("vibrationMode", VibrationMode.SHORT.name)
    val vibrationMode = try { VibrationMode.valueOf(vibModeStr) } catch (_: Exception) { VibrationMode.SHORT }
    val overrideDnd = optBoolean("overrideDnd", false)

    return AlertSignalConfig(
        enabled = enabled,
        displayMode = displayMode,
        vibrationMode = vibrationMode,
        overrideDnd = overrideDnd
    )
}

private fun alarmDefaultsToJson(map: Map<AlarmSeverity, AlertSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (severity, config) ->
        json.put(severity.name, config.toJson())
    }
    return json.toString()
}

private fun parseAlarmDefaults(jsonStr: String?): Map<AlarmSeverity, AlertSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmSeverity, AlertSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val severity = try { AlarmSeverity.valueOf(key) } catch (_: Exception) { null }
            if (severity != null) {
                result[severity] = json.getJSONObject(key).toAlertSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}

private fun alarmOverridesToJson(map: Map<AlarmType, AlertSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (type, config) ->
        json.put(type.name, config.toJson())
    }
    return json.toString()
}

private fun parseAlarmOverrides(jsonStr: String?): Map<AlarmType, AlertSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmType, AlertSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val type = try { AlarmType.valueOf(key) } catch (_: Exception) { null }
            if (type != null) {
                result[type] = json.getJSONObject(key).toAlertSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}

private fun recommendationOverridesToJson(map: Map<RecommendationType, AlertSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (type, config) ->
        json.put(type.name, config.toJson())
    }
    return json.toString()
}

private fun parseRecommendationOverrides(jsonStr: String?): Map<RecommendationType, AlertSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<RecommendationType, AlertSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val type = try { RecommendationType.valueOf(key) } catch (_: Exception) { null }
            if (type != null) {
                result[type] = json.getJSONObject(key).toAlertSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}

private fun reminderOverridesToJson(map: Map<ReminderType, AlertSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (type, config) ->
        json.put(type.name, config.toJson())
    }
    return json.toString()
}

private fun parseReminderOverrides(jsonStr: String?): Map<ReminderType, AlertSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<ReminderType, AlertSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val type = try { ReminderType.valueOf(key) } catch (_: Exception) { null }
            if (type != null) {
                result[type] = json.getJSONObject(key).toAlertSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}