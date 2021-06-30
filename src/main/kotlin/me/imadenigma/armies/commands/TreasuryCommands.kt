package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.user.User


@CommandAlias("a|army")
@Subcommand("tr|treasury")
class TreasuryCommands : BaseCommand() {

    @Default
    fun default(user: User) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        user.msgCR("commands treasury balance",user.army.getBalance())
    }

    @Subcommand("give|add|deposit")
    @CommandCompletion("@amount")
    @Syntax("<amount>")
    fun deposit(user: User, amount: Int) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        if (user.getBalance() >= amount) {
            MainCommands.success(user, "treasury add", amount.toDouble())
            Army.armies.first { it.members.contains(user) }.deposit(amount.toDouble())
            user.withdraw(amount.toDouble())
        }else user.msg("&4You don't have enough money")
    }

    @Subcommand("withdraw|take|remove")
    @CommandCompletion("@amount")
    @Syntax("<amount>")
    fun withdraw(user: User, amount: Int) {
        if (!MainCommands.checkExistence(user,"treasury")) return
        if (!user.hasPermission(Permissions.WITHDRAW_BALANCE)) {
            user.msgC("commands treasury take need-permission")
            return
        }
        if (user.army.getBalance() < amount) {
            user.msgC("commands treasury take need-money")
            return
        }
        MainCommands.success(user, "treasury take")
        Army.armies.first { it.members.contains(user) }.withdraw(amount.toDouble())
        user.deposit(amount.toDouble())
    }
}