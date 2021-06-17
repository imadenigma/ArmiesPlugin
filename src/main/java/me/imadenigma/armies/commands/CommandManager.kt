package me.imadenigma.armies.commands

import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.PaperCommandManager
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import me.lucko.helper.utils.Players
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
        this.acf.registerCommand(MapCommand())
    }

    private fun registerContexts() {
        this.acf.commandContexts.registerIssuerAwareContext(User::class.java) {
            if (it.sender !is Player) throw InvalidCommandArgument("you must be player")
            return@registerIssuerAwareContext if (it.isOptional) {
                val x = kotlin.runCatching {
                    User.getByUUID(Players.getOffline(it.popFirstArg()).get().uniqueId)
                }
                x.getOrElse { null }
            } else {
                val x = kotlin.runCatching {
                    User.getByUUID((it.sender as Player).uniqueId)
                }
                x.getOrElse { throw InvalidCommandArgument("") }
            }
        }
    }

    private fun registerCompletions() {
        this.acf.commandCompletions.registerAsyncCompletion("army") {
            return@registerAsyncCompletion Army.armies.map { it.name }.toList()
        }

        this.acf.commandCompletions.registerAsyncCompletion("amount") {
            return@registerAsyncCompletion IntStream.range(0, 100).toList().map { it.toString() }
        }

        this.acf.commandCompletions.registerAsyncCompletion("local_user") {
            if (it.sender !is Player) return@registerAsyncCompletion mutableListOf<String>()
            val user = User.getByUUID(it.player.uniqueId)
            if (user.isOnArmy()) {
                return@registerAsyncCompletion user.getArmy().members.map { user1 -> user1.getPlayer()!!.name }
            }
            return@registerAsyncCompletion mutableListOf<String>()
        }

        this.acf.commandCompletions.registerAsyncCompletion("user") {
            if (it.sender !is Player) return@registerAsyncCompletion mutableListOf<String>()
            return@registerAsyncCompletion User.users.map { user -> Players.getOffline(user.uuid).get().name }
        }
    }

}