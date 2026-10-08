package de.dh.pump.omnipod.protocol.ble

import java.util.UUID

enum class CharacteristicType(val value: String) {
    CMD("1a7e2441-e3ed-4464-8b7e-751e03d0dc5f"),
    DATA("1a7e2442-e3ed-4464-8b7e-751e03d0dc5f");

    val uuid: UUID = UUID.fromString(value)

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("1a7e4024-e3ed-4464-8b7e-751e03d0dc5f")

        fun byValue(value: String): CharacteristicType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown Characteristic Type: $value")
    }
}