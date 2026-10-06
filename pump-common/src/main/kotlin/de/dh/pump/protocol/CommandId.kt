package de.dh.pump.protocol

@JvmInline
value class CommandId(val value: Int) {
    init {
        require(value in 0..0xffff) { "Command id must fit UInt16" }
    }
}