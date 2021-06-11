@file:Suppress("SENSELESS_COMPARISON")

package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import com.google.common.base.Preconditions
import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.guis.RankGui
import me.imadenigma.armies.user.Invite
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.impl.ManualFireTurret
import me.lucko.helper.Services
import me.lucko.helper.utils.Players
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.material.Skull
import java.util.*

@CommandAlias("a|army")
class MainCommands : BaseCommand() {

    @Subcommand("create")
    @Syntax("<name>")
    fun create(user: User, name: String) {
        Preconditions.checkNotNull(name)
        if (user.isOnArmy()) {
            user.msgC("commands create already-in-army")
            return
        }
        if (Army.armies.any { it.name.contains(name) || name.contains(it.name) }) {
            user.msg("&4Name taken")
            return
        }
        success(user, "create")
        user.rank = Rank.EMPEROR
        val block = user.getPlayer()!!.world.getBlockAt(user.getPlayer()!!.location)
        block.type = Material.BEACON
        block.state.update()
        Army(UUID.randomUUID(), name, user.uuid, core = block, home = block.location)
    }

    @Subcommand("sethome")
    @Description("set home's location of the army")
    fun sethome(user: User) {
        checkExistence(user, "sethome")
        if (!hasPermission(user, Permissions.SET_HOME, "sethome")) return
        Army.armies.first { it.members.contains(user) }.home = user.getPlayer()!!.location
        success(user, "sethome")
    }


    @Subcommand("home")
    @Description("teleport to your army's home")
    fun home(user: User) {
        if (!checkExistence(user, "home")) return
        if (!hasPermission(user, Permissions.HOME, "home")) return
        val army = Army.armies.first { it.members.contains(user) }
        val player = Bukkit.getPlayer(user.uuid)
        player.teleport(army.home)
        success(user, "home")
    }

    @Default
    @Subcommand("help")
    @HelpCommand
    fun help(user: User) {
        val config = Services.load(Configuration::class.java).language
        val lines = config.getNode("commands", "help").getList(TypeToken.of(String::class.java))
        lines.forEach {
            user.msg(it)
        }
    }

    @Subcommand("leave")
    @Description("leave your army")
    fun leave(user: User) {
        if (!checkExistence(user, "leave")) return
        if (!hasPermission(user, Permissions.LEAVE, "leave")) return
        success(user, "leave")
        user.getArmy().kickMember(user)
    }

    @Subcommand("join")
    @CommandCompletion("@army")
    @Syntax("<army>")
    @Description("join an army")
    fun join(user: User, armyName: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyName, true) } ?: run {
            user.msgC("commands join army-not-found")
            return
        }
        if (user.rank != Rank.NOTHING) {
            user.msgC("commands join already-in-army")
            return
        }

        if (!army.isOpened) {
            val targetArm = Invite.allInvites.firstOrNull { it.receiver == user }?.army
            if (targetArm == army) {
                army.addMember(user)
                success(user, "join")
                return
            }
            user.msgC("commands join army-closed")
            return
        }
        army.addMember(user)
        success(user, "join")
    }

    @Subcommand("open")
    @Description("open your army to make players can join")
    fun open(user: User) {
        if (!checkExistence(user, "open")) return
        if (!hasPermission(user, Permissions.OPEN_OR_CLOSE, "open")) return
        user.getArmy().isOpened = true
        success(user, "open")
    }

    @Subcommand("close")
    @Description("close your army")
    fun close(user: User) {
        if (!checkExistence(user, "close")) return
        if (!hasPermission(user, Permissions.OPEN_OR_CLOSE, "close")) return
        user.getArmy().isOpened = false
        success(user, "close")
    }


    @Subcommand("name")
    @Description("returns the name of your army")
    fun name(user: User) {
        if (!checkExistence(user, "name")) return
        user.msgCR("commands name found", user.getArmy().name)
    }

    @Subcommand("names")
    @Description("names of all armies")
    fun names(user: User) {
        Army.armies.forEach { user.msg("&a${it.name}") }
    }

    @Subcommand("invite")
    @CommandCompletion("@user")
    @Syntax("<user>")
    @Description("invite your mate to join your army")
    fun invite(user: User, targetStr: String?) {
        if (!checkExistence(user, "invite")) return
        if (!hasPermission(user, Permissions.OPEN_OR_CLOSE, "invite")) return
        val target =
            User.users.filter { it.getPlayer() != null }.firstOrNull { it.getPlayer()!!.name.equals(targetStr, true) }
                ?: kotlin.run {
                    user.msgCR("commands invite player-not-found", targetStr ?: "")
                    return
                }
        if (user == target) {
            user.msg("&4You cannot invite yourself")
            return
        }
        println("user: $user &&&&& target: $target")
        if (target.isOnArmy()) {
            if (target.getArmy() == user.getArmy()) {
                user.msgC("commands invite same-army")
                return
            }
        }
        val invite = Invite(user, target, user.getArmy())
        val msg = TextComponent("Click Here to accept")
        msg.color = ChatColor.GREEN
        msg.isBold = true
        msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/a join ${user.getArmy().name}")

        invite.sender.msgCR(
            "commands invite sender-msg",
            invite.receiver.getPlayer()!!.displayName,
            user.getArmy().name
        )
        invite.receiver.msgCR(
            "commands invite receiver-msg",
            invite.sender.getPlayer()!!.displayName,
            user.getArmy().name
        )
        if (Players.getOffline(target.uuid).isPresent) target.getPlayer()!!.spigot().sendMessage(msg)
    }

    @Subcommand("perms")
    @Description("add or take a permission from a rank")
    fun perms(user: User) {
        if (!checkExistence(user, "perms")) return
        if (!hasPermission(user, Permissions.PERM, "perms")) return
        RankGui(user)
    }

    @Subcommand("rank")
    @Description("display your rank")
    fun rank(user: User) {
        if (!checkExistence(user, "rank")) return
        user.msg("&3You rank is: ${user.rank}")
        ManualFireTurret(user.getPlayer()!!.location, user.getArmy(), 100, 100.0, 0.0, 0.0, 1, UUID.randomUUID())
    }

    //                              //
    //         NEED FIXING          //
    //                              //

    @Subcommand("promote")
    @Description("promote a member to another rank")
    @Syntax("[target]")
    @CommandCompletion("@local_user")
    fun promote(user: User, @Optional targetName: String?) {
        if (!checkExistence(user, "promote")) return
        if (!hasPermission(user, Permissions.PROMOTE, "promote")) return
        val target = User.users.stream()
            .filter { Objects.nonNull(it.getPlayer()) }
            .filter { it.getPlayer()!!.name.equals(targetName, true) }
            .findAny()
        Skull()
        if (!target.isPresent) {
            if (hasPermission(user, Permissions.PROMOTE, "promote")) {
                if (user.rank == Rank.EMPEROR) {
                    user.msgC("commands promote user emperor")
                    return
                }
                if (user.rank == Rank.KNIGHT) {
                    user.msgC("commands promote user knight")
                    return
                }
                val nextRank = getNextRank(user.rank)!!
                user.rank = nextRank
                success(user, "promote user", "", user.rank.name)
            }
            return
        }
        if (user.hasPermission(Permissions.PROMOTE)) {
            if (target.get().rank == Rank.EMPEROR) {
                user.msgC("commands promote target emperor")
                return
            }
            if (target.get().rank == Rank.KNIGHT) {
                user.msgC("commands promote target knight")
                return
            }
            val nextRank = getNextRank(target.get().rank)
            target.get().rank = nextRank!!
            success(user, "promote target", Players.getOffline(target.get().uuid).get().name, target.get().rank.name)
            user.msgCR("commands promote user-msg", user.getPlayer()!!.displayName, target.get().rank.name)
        }
    }


    companion object {

        fun checkExistence(user: User, command: String): Boolean {
            return if (user.rank == Rank.NOTHING) {
                user.msgC("commands $command not-in-army")
                false
            } else if (!Army.armies.any { it.members.contains(user) }) {
                user.msgC("commands $command not-in-army")
                false
            } else true
        }

        fun success(user: User, command: String, vararg rep: Any) {
            user.msgCR("commands $command success", rep)
        }

        fun hasPermission(user: User, permission: Permissions, command: String): Boolean {
            if (user.hasPermission(permission)) return true
            user.msgC("commands $command need-permission")
            return false
        }

        fun getNextRank(rank: Rank): Rank? {
            if (rank == Rank.KNIGHT) return Rank.EMPEROR
            if (rank == Rank.SOLDIER) return Rank.KNIGHT
            if (rank == Rank.PEASANT) return Rank.SOLDIER
            if (rank == Rank.PRISONER) return Rank.PEASANT
            return null
        }
    }
}