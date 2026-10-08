package de.dh.pump.omnipod.protocol.transport

interface BleCharacteristicIO {
    fun receivePacket(timeoutMs: Long = DEFAULT_IO_TIMEOUT_MS): ByteArray?
    fun sendAndConfirmPacket(payload: ByteArray): BleSendResult
    fun flushIncomingQueue(): Boolean
    fun readyToRead(): BleSendResult

    companion object {
        const val DEFAULT_IO_TIMEOUT_MS = 1000L
    }
}

interface CmdBleIO : BleCharacteristicIO {
    fun peekCommand(): ByteArray?
    fun hello(): BleSendResult
    fun expectCommandType(expected: BleCommand, timeoutMs: Long = BleCharacteristicIO.DEFAULT_IO_TIMEOUT_MS): BleConfirmResult
}

interface DataBleIO : BleCharacteristicIO