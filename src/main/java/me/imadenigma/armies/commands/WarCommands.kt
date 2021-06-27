package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.army.Invade
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.user.User
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
        user.getArmy().enemies.add(army)
        army.enemies.add(user.getArmy())
        user.getArmy().msgCR("army-msgs enemy sender", army.name)
        army.msgCR("army-msgs enemy receiver", user.getArmy().name)
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
        val victim = user.getArmy()
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
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        if (ArmyManager.consoleArmies.contains(army)) {
            user.msg("&cYou cannot invade a console army !")
            return
        }
        if (!MainCommands.checkExistence(user, "invade")) return
        if (!MainCommands.hasPermission(user, Permissions.INVADE, "invade")) return
        val invade = Invade(user.getArmy().uuid, army.uuid)
        user.getArmy().invades.add(invade)
        army.invades.add(invade)
        army.msgCR("army-msgs invade receiver", user.getArmy().name)
        user.getArmy().msgCR("army-msgs invade sender", army.name)
        val damagerMembers = mutableSetOf<Player>()
        damagerMembers.addAll(user.getArmy().members.mapNotNull { it.getPlayer() })
        damagerMembers.addAll(user.getArmy().prisoners.mapNotNull { it.getPlayer() })
        damagerMembers.addAll(army.members.mapNotNull { it.getPlayer() })
        damagerMembers.addAll(army.prisoners.mapNotNull { it.getPlayer() })
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
                        damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("$i seconds left")) }
                        if (i <= 0) {
                            if (!army.isDisbanded) {
                                army.kill(user.getArmy())
                            }
                            this.cancel()
                            this.close()
                        }
                    }, 0L, TimeUnit.SECONDS, 1L, TimeUnit.SECONDS)

                }, TimeUnit.SECONDS.toMillis(5) - TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS)

        }
        with(army) {
            this.attackersBB.addPlayers(user.getArmy().members.mapNotNull { it.getPlayer() })
            this.defendersBB.addPlayers(this.members.mapNotNull { it.getPlayer() })
            user.getArmy().invades.add(invade)
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
        if (user.getArmy().allies.contains(army)) {
            user.msg("&cYou did already make a coalition with ${army.name}")
            return
        }
        val msg = TextComponent("Click here to accept")
        msg.color = ChatColor.GREEN
        msg.isBold = true
        msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/army acc,ept ${user.getArmy().name}")
        val own = User.getByUUID(army.owner)
        val owner = own.getPlayer() ?: run {
            user.msg("&3The owner of this army is offline, try later")
            return
        }
        own.msgCR("commands coalition receiver", Players.get(user.uuid).get().displayName, user.getArmy().name)
        owner.spigot().sendMessage(msg)
        user.msgCR("commands coalition sender", own.getArmy().name)
    }

    @Private
    @Subcommand("acc,ept")
    @Syntax("<army>")
    fun accept(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        user.getArmy().allies.add(army)
        army.allies.add(user.getArmy())
        army.msgCR("army-msgs coalition", user.getArmy().name)
        user.getArmy().msgCR("army-msgs coalition", army.name)
    }
}