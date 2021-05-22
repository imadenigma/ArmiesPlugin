package me.imadenigma.armies.commands

import co.aikar.commands.PaperCommandManager
import com.google.common.collect.Lists
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
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

        this.acf.commandContexts.registerContext(User::class.java) {
            return@registerContext User.getByUUID(Bukkit.getPlayer(it.popFirstArg()).uniqueId)
        }
    }

    private fun registerCompletions() {
        this.acf.commandCompletions.registerAsyncCompletion("army") {
            return@registerAsyncCompletion Army.armies.map { it.name }.toList()
        }

        this.acf.commandCompletions.registerAsyncCompletion("amount") {
            return@registerAsyncCompletion IntStream.range(0,100).toList().map { it.toString() }
        }

        this.acf.commandCompletions.registerAsyncCompletion("local_user") {
            if (it.sender !is Player) return@registerAsyncCompletion mutableListOf<String>()
            val user = User.getByUUID(it.player.uniqueId)
            if (user.isOnArmy()) {
                return@registerAsyncCompletion user.getArmy().members.map { user1 -> user1.getPlayer().name}
            }
            return@registerAsyncCompletion  mutableListOf<String>()
        }
    }

}