package de.dh.pump.dana.protocol

enum class DanaRsEncryptionType(val type: Int) {
    ENCRYPTION_DEFAULT(0),
    ENCRYPTION_RSv3(1),
    ENCRYPTION_BLE5(2)
}