package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.user.User


@CommandAlias("a|army")
@Subcommand("tr|treasury")
class TreasuryCommands : BaseCommand() {

    @Default
    fun default(user: User) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        user.msgCR("commands treasury balance",user.getArmy().getBalance())
    }

    @Subcommand("give|add|deposit")
    @CommandCompletion("@amount")
    @Syntax("<amount>")
    fun deposit(user: User, amount: Int) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        if (user.rank != Rank.EMPEROR) {
            user.msgC("commands treasury add need-permission")
            return
        }
        MainCommands.success(user, "treasury add")
        Army.armies.first { it.members.contains(user) }.deposit(amount.toDouble())
        user.withdraw(amount.toDouble())
    }

    @Subcommand("withdraw|take|remove")
    @CommandCompletion("@amount")
    @Syntax("<amount>")
    fun withdraw(user: User, amount: Int) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        if (user.rank != Rank.EMPEROR && user.rank != Rank.KNIGHT) {
            user.msgC("commands treasury take need-permission")
            return
        }
        MainCommands.success(user, "treasury take")
        Army.armies.first { it.members.contains(user) }.withdraw(amount.toDouble())
        user.deposit(amount.toDouble())
    }

}