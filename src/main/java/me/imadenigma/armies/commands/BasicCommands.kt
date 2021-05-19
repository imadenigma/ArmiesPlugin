package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.common.base.Preconditions
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import java.util.*

@CommandAlias("a|army")
class BasicCommands : BaseCommand() {

    @Default
    fun default(user: User) {
        if (user.isOnArmy()) user.msgCR("commands army found")
        else user.msgC("commands army notfound")
    }

    @Subcommand("create")
    @Syntax("<name>")
    fun create(user: User, name: String) {
        Preconditions.checkNotNull(name)
        if (user.isOnArmy()) {
            user.msgC("commands create already-in-army")
            return
        }
        user.msgC("commands create success")
        Army(UUID.randomUUID(),name,user.offlinePlayer.uniqueId)
    }

}