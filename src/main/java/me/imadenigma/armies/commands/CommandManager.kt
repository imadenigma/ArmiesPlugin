package me.imadenigma.armies.commands

import co.aikar.commands.PaperCommandManager
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import org.bukkit.entity.Player
import java.util.stream.IntStream
import kotlin.streams.toList

class CommandManager {

   private val acf: PaperCommandManager

    init {
        this.acf = PaperCommandManager(Helper.hostPlugin())
        registerContexts()
        registerCompletions()
        registerCommands()
    }

    private fun registerCommands() {
        this.acf.registerCommand(MainCommands())
        this.acf.registerCommand(TreasuryCommands())
        this.acf.registerCommand(WarCommands())
    }

    private fun registerContexts() {
        this.acf.commandContexts.registerIssuerAwareContext(User::class.java) {
            if (it.sender !is Player) return@registerIssuerAwareContext null
            return@registerIssuerAwareContext User.getByUUID(it.player.uniqueId)
        }
        this.acf.commandContexts.registerContext(Army::class.java) {
            return@registerContext Army.armies.firstOrNull { army ->  army.name.equals(it.popFirstArg(),true) }
        }
    }

    private fun registerCompletions() {
        this.acf.commandCompletions.registerAsyncCompletion("army") {
            return@registerAsyncCompletion Army.armies.map { it.name }.toList()
        }

        this.acf.commandCompletions.registerAsyncCompletion("amount") {
            return@registerAsyncCompletion IntStream.range(0,100).toList().map { it.toString() }
        }
    }

}