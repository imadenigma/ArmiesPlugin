package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.army.Invade
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Schedulers
import me.lucko.helper.promise.Promise
import me.lucko.helper.utils.Players
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

@CommandAlias("a|army")
class WarCommands : BaseCommand() {

    @Subcommand("enemy")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun enemy(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        if (ArmyManager.consoleArmies.contains(army)) {
            user.msg("&cYou cannot enemy a console army !")
            return
        }
        if (!MainCommands.checkExistence(user, "enemy")) return
        if (!MainCommands.hasPermission(user, Permissions.ENEMY, "enemy")) return
        user.army.enemies.add(army)
        army.enemies.add(user.army)
        user.army.msgCR("army-msgs enemy sender", army.name)
        army.msgCR("army-msgs enemy receiver", user.army.name)
    }

    @Subcommand("surrender")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun surrender(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        if (ArmyManager.consoleArmies.contains(army)) {
            user.msg("&cYou cannot surrender to a console army !")
            return
        }
        if (!MainCommands.checkExistence(user, "surrender")) return
        if (!MainCommands.hasPermission(user, Permissions.SURRENDER, "surrender")) return
        val victim = user.army
        victim.msgCR("army-msgs surrender sender", army.name)
        army.msgCR("army-msgs surrender receiver", victim.name)
        army.deposit(victim.getBalance())
        army.invades.removeAll(
                army.invades.filter { (it.attacker.equals(army.uuid) && it.defender.equals(victim.uuid)) || (it.defender.equals(army.uuid) && it.attacker.equals(victim.uuid)) }
        )
        victim.invades.removeAll(
                army.invades.filter { (it.attacker.equals(army.uuid) && it.defender.equals(victim.uuid)) || (it.defender.equals(army.uuid) && it.attacker.equals(victim.uuid)) }
        )
        army.kill(victim)
    }

    @Subcommand("invade")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun invade(user: User, armyStr: String) {
        if (!MainCommands.checkExistence(user, "invade")) return
        if (user.army.getBalance() < 100000) {
            user.msg("&4You must have at least 100K in treasury")
            return
        }
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        if (ArmyManager.consoleArmies.contains(army)) {
            user.msg("&cYou cannot invade a console army !")
            return
        }
        if ((System.currentTimeMillis() - user.army.lastCoreHolding) / 1000 < TimeUnit.HOURS.toSeconds(1)) {
            user.msg("${TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - user.army.lastCoreHolding)} Minutes left to invade")
            return
        }
        if (!MainCommands.hasPermission(user, Permissions.INVADE, "invade")) return
        val invade = Invade(user.army.uuid, army.uuid)
        user.army.invades.add(invade)
        user.army.lastInvading = System.currentTimeMillis()
        army.invades.add(invade)
        army.msgCR("army-msgs invade receiver", user.army.name)
        user.army.msgCR("army-msgs invade sender", army.name)
        val damagerMembers = mutableSetOf<Player>()
        damagerMembers.addAll(user.army.members.mapNotNull { it.player })
        damagerMembers.addAll(user.army.prisoners.mapNotNull { it.player })
        damagerMembers.addAll(army.members.mapNotNull { it.player })
        damagerMembers.addAll(army.prisoners.mapNotNull { it.player })
        damagerMembers.forEach { if (!army.isDisbanded) it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("30 minutes left")) }
        with(Promise.start()) {
            army.addPromise(this)
            this.thenRunDelayedSync({ if (!army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("20 minutes left")) } }, 10L, TimeUnit.SECONDS)
                    .thenRunDelayedSync({ if (!army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("10 minutes left")) } }, 10L, TimeUnit.SECONDS)
                    .thenRunDelayedSync({ if (!army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("5 minutes left")) } }, 5L, TimeUnit.SECONDS)
                    .thenRunDelayedSync({
                        var i = 11

                        Schedulers.sync().runRepeating({ task ->
                            i--
                            if (i <= 0) {
                                if (!army.isDisbanded) {
                                    //sdaasd
                                    Army.armies.forEach { it.invades.remove(invade) }
                                    army.deposit(user.army.getBalance() * 0.4)
                                    user.army.withdraw(user.army.getBalance() * 0.4)
                                    damagerMembers.forEach { it.sendMessage("&6Invading ended !!".colorize()) }
                                }
                                this.cancel()
                                this.close()
                                task.stop()
                                task.close()
                            } else if (!army.isDisbanded) {
                                damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("$i seconds left")) }
                            }
                        }, 0L, TimeUnit.SECONDS, 1L, TimeUnit.SECONDS)

                    }, TimeUnit.SECONDS.toMillis(5) - TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS)

        }
        with(army) {
            this.attackersBB.addPlayers(user.army.members.mapNotNull { it.player })
            this.defendersBB.addPlayers(this.members.mapNotNull { it.player })
            user.army.invades.add(invade)
            this.invades.add(invade)
        }
    }

    @Subcommand("coalition")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun coalition(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }

        if (ArmyManager.consoleArmies.contains(army)) {
            user.msg("&cYou cannot make coalition with a console army !")
            return
        }

        if (!MainCommands.checkExistence(user, "coalition")) return
        if (!MainCommands.hasPermission(user, Permissions.INVADE, "coalition")) return
        if (user.army.allies.contains(army)) {
            user.msg("&cYou did already make a coalition with ${army.name}")
            return
        }
        val msg = TextComponent("Click here to accept")
        msg.color = ChatColor.GREEN
        msg.isBold = true
        msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/army acc,ept ${user.army.name}")
        val own = User.getByUUID(army.owner)
        val owner = own.player ?: run {
            user.msg("&3The owner of this army is offline, try later")
            return
        }
        own.msgCR("commands coalition receiver", Players.get(user.uuid).get().displayName, user.army.name)
        owner.spigot().sendMessage(msg)
        user.msgCR("commands coalition sender", own.army.name)
    }

    @Private
    @Subcommand("acc,ept")
    @Syntax("<army>")
    fun accept(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        user.army.allies.add(army)
        army.allies.add(user.army)
        army.msgCR("army-msgs coalition", user.army.name)
        user.army.msgCR("army-msgs coalition", army.name)
    }
}