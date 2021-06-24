package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Invade
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Schedulers
import me.lucko.helper.promise.Promise
import me.lucko.helper.scheduler.Task
import me.lucko.helper.utils.Players
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
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
        if (!MainCommands.checkExistence(user, "surrender")) return
        if (!MainCommands.hasPermission(user, Permissions.SURRENDER, "surrender")) return
        val victim = user.getArmy()
        victim.msgCR("army-msgs surrender sender", army.name)
        army.msgCR("army-msgs surrender receiver", victim.name)
        army.deposit(victim.getBalance())
        victim.core.let {
            it.type = Material.AIR
            it.state.update()
        }
        victim.members.forEach {
            army.prisoners.add(it)
            it.rank = Rank.PRISONER
        }
        victim.prisoners.forEach {
            army.prisoners.add(it)
            it.rank = Rank.PRISONER
        }
        victim.lands.forEach {
            army.lands.add(it)
        }
        victim.turrets.forEach {
            army.turrets.add(it)
            Turrets.allTurrets.firstOrNull { arm -> arm.uuid == it }?.army = army
        }
        victim.disband()
    }

    @Subcommand("invade")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun invade(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
            return
        }
        if (!MainCommands.checkExistence(user, "invade")) return
        if (!MainCommands.hasPermission(user, Permissions.INVADE, "invade")) return
        val invade = Invade(user.getArmy().uuid, army.uuid)
        user.getArmy().invades.add(invade)
        army.invades.add(invade)
        army.msgCR("army-msgs invade receiver", user.getArmy().name)
        user.getArmy().msgCR("army-msgs invade sender", army.name)
        var i = 1
        val tasks = mutableSetOf<Task>()
        val promises = mutableSetOf<Promise<Void>>()
        tasks.add(Schedulers.sync()
            .runRepeating({ _ ->
                if (i == 3) {
                    army.members.mapNotNull { it.getPlayer() }
                        .forEach { it.sendTitle("10 Minutes left for invading", "", 30, 70, 20) }
                    promises.add(Schedulers.async().runLater({
                        army.members.mapNotNull { it.getPlayer() }.forEach { it.sendTitle("5 Minutes left for invading", "", 30, 70, 20) }
                        promises.add(
                            Schedulers.async().runLater({
                                var x = 5
                                Schedulers.async().runRepeating({ task ->
                                    run {
                                        army.members.mapNotNull { it.getPlayer() }
                                            .forEach {
                                                it.sendTitle(
                                                    "$x seconds left for invading",
                                                    "",
                                                    30,
                                                    70,
                                                    20
                                                )
                                            }
                                        x--
                                        if (x == 0) {
                                            army.kill(user.getArmy())
                                            task.close()
                                        }
                                    }
                                }, 0L, TimeUnit.SECONDS, 1L, TimeUnit.SECONDS)
                            }, (TimeUnit.MINUTES.toMillis(4) + TimeUnit.SECONDS.toMillis(45)))
                        )
                    }, 5L, TimeUnit.MINUTES))
                }else {
                    army.members.mapNotNull { it.getPlayer() }
                        .forEach { it.sendTitle("${30 - i * 10} Minutes left for invading", "", 30, 70, 20) }
                    i++
                }
            }, 0L,TimeUnit.MINUTES, 10L, TimeUnit.MINUTES)
        )
        invade.tasks.addAll(tasks)
        invade.promises.addAll(promises)
    }

    @Subcommand("coalition")
    @CommandCompletion("@army")
    @Syntax("<army>")
    fun coalition(user: User, armyStr: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyStr, true) } ?: run {
            user.msg("&cArmy not found")
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