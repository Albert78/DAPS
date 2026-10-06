package de.dh.pump.dana.commands.etc

import de.dh.pump.dana.commands.DanaRsAckPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import de.dh.pump.dana.commands.le16
import de.dh.pump.protocol.ByteWriter

class EtcKeepConnectionCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.ETC_KEEP_CONNECTION)

class EtcSetHistorySaveCommand(
    private val historyType: Int,
    private val historyYear: Int,
    private val historyMonth: Int,
    private val historyDate: Int,
    private val historyHour: Int,
    private val historyMinute: Int,
    private val historySecond: Int,
    private val historyCode: Int,
    private val historyValue: Int,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.ETC_SET_HISTORY_SAVE) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(historyType)
        writer.writeUInt8(historyYear)
        writer.writeUInt8(historyMonth)
        writer.writeUInt8(historyDate)
        writer.writeUInt8(historyHour)
        writer.writeUInt8(historyMinute)
        writer.writeUInt8(historySecond)
        writer.writeUInt8(historyCode)
        writer.writeBytes(le16(historyValue))
    }
}