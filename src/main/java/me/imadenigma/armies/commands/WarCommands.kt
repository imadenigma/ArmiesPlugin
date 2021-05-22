package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.user.User
import co.aikar.commands.annotation.Default

@CommandAlias("a|army")
class WarCommands : BaseCommand() {

    // TODO: 21/05/2021
    @Subcommand("enemy")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun enemy(user: User, army: Army) {
        if (!MainCommands.checkExistence(user, "enemy")) return
        if (user.rank != Rank.EMPEROR) {
            user.msgC("commands enemy not-emperor")
            return
        }

    }


}