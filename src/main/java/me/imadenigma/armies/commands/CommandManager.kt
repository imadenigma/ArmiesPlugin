package me.imadenigma.armies.commands

import co.aikar.commands.PaperCommandManager
import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import org.bukkit.entity.Player

@Suppress("JoinDeclarationAndAssignment")
class CommandManager {
    val acf: PaperCommandManager

    init {
        this.acf = PaperCommandManager(Helper.hostPlugin())
        registerContexts()
        registerCommands()
    }

    private fun registerCommands() {
        this.acf.registerCommand(BasicCommands())
    }

    private fun registerContexts() {
        this.acf.commandContexts.registerContext(User::class.java) {
            if (it.sender !is Player) return@registerContext null
            return@registerContext User.getByUUID(it.player.uniqueId)
        }
    }

}