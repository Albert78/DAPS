package de.dh.pump.client.singleframe

@JvmInline
value class CommandId(val value: Int) {
    init {
        require(value in 0..0xffff) { "Command id must fit UInt16" }
    }
}