package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSignalConfig
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.AlertDisplayMode
import de.dh.daps.common.model.data.SoundConfig
import de.dh.daps.common.model.data.VibrationMode
import de.dh.daps.core.repository.db.entities.AlarmProfileEntity
import org.json.JSONObject

fun AlarmProfileEntity.toModel(): AlarmProfile {
    return AlarmProfile(
        id = id,
        name = name,
        isDefault = is_default,
        severityDefaults = parseSeverityDefaults(severity_defaults_json),
        customOverrides = parseCustomOverrides(custom_overrides_json)
    )
}

fun AlarmProfile.toEntity(isActive: Boolean = false): AlarmProfileEntity {
    return AlarmProfileEntity(
        id = id,
        name = name,
        is_default = isDefault,
        is_active = isActive,
        severity_defaults_json = severityDefaultsToJson(severityDefaults),
        custom_overrides_json = customOverridesToJson(customOverrides)
    )
}

private fun AlarmSignalConfig.toJson(): JSONObject {
    val json = JSONObject()
    val isFS = isFullScreen
    json.put("isFullScreen", isFS)
    val sound = soundConfig
    if (sound != null) {
        json.put("volume", sound.volume)
        json.put("soundUri", sound.soundUri ?: JSONObject.NULL)
    }
    json.put("vibrationMode", vibrationMode.name)
    json.put("overrideDnd", overrideDnd)
    return json
}

private fun JSONObject.toAlarmSignalConfig(): AlarmSignalConfig {
    val isFS = optBoolean("isFullScreen", false)
    val displayMode = if (isFS) {
        val volume = optInt("volume", 80)
        val soundUri = if (isNull("soundUri") || !has("soundUri")) null else optString("soundUri")
        AlertDisplayMode.FullScreen(sound = SoundConfig(volume = volume, soundUri = soundUri))
    } else {
        AlertDisplayMode.NotificationOnly
    }

    val vibModeStr = optString("vibrationMode", VibrationMode.SHORT.name)
    val vibrationMode = try { VibrationMode.valueOf(vibModeStr) } catch (_: Exception) { VibrationMode.SHORT }
    val overrideDnd = optBoolean("overrideDnd", false)

    return AlarmSignalConfig(
        displayMode = displayMode,
        vibrationMode = vibrationMode,
        overrideDnd = overrideDnd
    )
}

private fun severityDefaultsToJson(map: Map<AlarmSeverity, AlarmSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (severity, config) ->
        json.put(severity.name, config.toJson())
    }
    return json.toString()
}

private fun parseSeverityDefaults(jsonStr: String?): Map<AlarmSeverity, AlarmSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmSeverity, AlarmSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val severity = try { AlarmSeverity.valueOf(key) } catch (_: Exception) { null }
            if (severity != null) {
                result[severity] = json.getJSONObject(key).toAlarmSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}

private fun customOverridesToJson(map: Map<AlarmType, AlarmSignalConfig>): String {
    val json = JSONObject()
    map.forEach { (type, config) ->
        json.put(type.name, config.toJson())
    }
    return json.toString()
}

private fun parseCustomOverrides(jsonStr: String?): Map<AlarmType, AlarmSignalConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmType, AlarmSignalConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val type = try { AlarmType.valueOf(key) } catch (_: Exception) { null }
            if (type != null) {
                result[type] = json.getJSONObject(key).toAlarmSignalConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}