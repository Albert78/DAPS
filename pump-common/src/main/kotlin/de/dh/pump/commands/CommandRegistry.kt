package de.dh.pump.commands

import de.dh.pump.protocol.CommandId

class CommandRegistry(commands: Iterable<PumpCommand<*>>) {
    private val byId: Map<CommandId, PumpCommand<*>> = commands.associateBy { it.commandId }

    fun find(commandId: CommandId): PumpCommand<*>? = byId[commandId]

    fun requireKnown(commandId: CommandId): PumpCommand<*> {
        return find(commandId) ?: error("Unknown command id: 0x${commandId.value.toString(16)}")
    }
}