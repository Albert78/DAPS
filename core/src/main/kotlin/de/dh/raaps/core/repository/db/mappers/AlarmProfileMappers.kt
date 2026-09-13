package de.dh.raaps.core.repository.db.mappers

import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.AlarmType
import de.dh.raaps.common.model.data.VibrationMode
import de.dh.raaps.core.repository.db.entities.AlarmProfileEntity
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

private fun AlarmSoundConfig.toJson(): JSONObject {
    val json = JSONObject()
    json.put("volume", volume)
    json.put("soundUri", soundUri ?: JSONObject.NULL)
    json.put("vibrationMode", vibrationMode.name)
    json.put("overrideDnd", overrideDnd)
    return json
}

private fun JSONObject.toAlarmSoundConfig(): AlarmSoundConfig {
    val volume = optInt("volume", 80)
    val soundUri = if (isNull("soundUri")) null else optString("soundUri")
    val vibModeStr = optString("vibrationMode", VibrationMode.SHORT.name)
    val vibrationMode = try { VibrationMode.valueOf(vibModeStr) } catch (_: Exception) { VibrationMode.SHORT }
    val overrideDnd = optBoolean("overrideDnd", false)
    return AlarmSoundConfig(
        volume = volume,
        soundUri = soundUri,
        vibrationMode = vibrationMode,
        overrideDnd = overrideDnd
    )
}

private fun severityDefaultsToJson(map: Map<AlarmSeverity, AlarmSoundConfig>): String {
    val json = JSONObject()
    map.forEach { (severity, config) ->
        json.put(severity.name, config.toJson())
    }
    return json.toString()
}

private fun parseSeverityDefaults(jsonStr: String?): Map<AlarmSeverity, AlarmSoundConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmSeverity, AlarmSoundConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val severity = try { AlarmSeverity.valueOf(key) } catch (_: Exception) { null }
            if (severity != null) {
                result[severity] = json.getJSONObject(key).toAlarmSoundConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}

private fun customOverridesToJson(map: Map<AlarmType, AlarmSoundConfig>): String {
    val json = JSONObject()
    map.forEach { (type, config) ->
        json.put(type.name, config.toJson())
    }
    return json.toString()
}

private fun parseCustomOverrides(jsonStr: String?): Map<AlarmType, AlarmSoundConfig> {
    if (jsonStr.isNullOrEmpty()) return emptyMap()
    val result = mutableMapOf<AlarmType, AlarmSoundConfig>()
    try {
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val type = try { AlarmType.valueOf(key) } catch (_: Exception) { null }
            if (type != null) {
                result[type] = json.getJSONObject(key).toAlarmSoundConfig()
            }
        }
    } catch (_: Exception) {}
    return result
}