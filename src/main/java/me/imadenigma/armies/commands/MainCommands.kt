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
import me.imadenigma.armies.guis.ShopGui
import me.imadenigma.armies.user.Invite
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.impl.FireballTurret
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.Services
import org.bukkit.Bukkit
import org.bukkit.Material
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
        success(user, "create")
        user.rank = Rank.EMPEROR
        val block = user.getPlayer()!!.world.getBlockAt(user.getPlayer()!!.location)
        block.type = Material.BEACON
        block.state.update()
        Army(UUID.randomUUID(), name, user.uuid, core = block, home = block.location)
    }


    @Subcommand("sethome")
    @Description("set home's me.imadenigma.armies.weapons.impl.getLocation of the army")
    fun sethome(user: User) {
        checkExistence(user, "sethome")
        if (!hasPermission(user, Permissions.SET_HOME, "sethome")) return
        Army.armies.first { it.owner == user.uuid }.home = user.getPlayer()!!.location
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
    fun join(user: User, army: Army) {
        if (user.rank != Rank.NOTHING) {
            user.msgC("commands join already-in-army")
            return
        }
        if (!army.isOpened) {
            if (Invite.allInvites.any { it.receiver == user }) {
                if (Invite.allInvites.first { it.receiver.uuid == user.uuid }.army == army) {
                    user.msgC("commands join success")
                    army.addMember(user)
                    success(user, "join")
                    return
                }
            }
            user.msgC("commands join army-closed")
            return
        }
        user.msgC("commands join success")
        army.addMember(user)
        success(user, "join")
    }


    @Subcommand("promote")
    @Description("promote a member to another rank")
    @CommandCompletion("@local_user")
    fun promote(user: User, @Flags("other") @Optional target: User) {
        if (!hasPermission(user, Permissions.PROMOTE, "promote")) return
        if (!checkExistence(user, "promote")) return
        if (target == null) {
            if (user.rank == Rank.EMPEROR) {
                user.msgC("commands promote emperor")
                return
            }
            if (user.rank == Rank.KNIGHT) {
                user.msgC("commands promote knight")
                return
            }
            if (user.hasPermission(Permissions.PROMOTE)) {
                var index = Rank.sorted.indexOf(target.rank) + 1
                if (index == Rank.sorted.size || index == Rank.sorted.size - 1) index = Rank.sorted.size - 2
                target.rank = Rank.sorted[index]
                success(user, "promote", target.getPlayer()!!.displayName, target.rank.name)
                user.msgCR("commands promote user-msg", user.getPlayer()!!.displayName, target.rank.name)
            }
        } else {
            if (target.rank == Rank.KNIGHT || target.rank == Rank.EMPEROR) {
                user.msg("failed")
                return
            }
            var index = Rank.sorted.indexOf(target.rank) + 1
            if (index == Rank.sorted.size || index == Rank.sorted.size - 1) index = Rank.sorted.size - 2
            target.rank = Rank.sorted[index]
            success(user, "promote", target.getPlayer()!!.displayName, target.rank.name)
            user.msgCR("commands promote user-msg", user.getPlayer()!!.displayName, target.rank.name)
        }
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

    @Subcommand("invite")
    @CommandCompletion("@user")
    @Syntax("<user>")
    @Description("invite your mate to join your army")
    fun invite(user: User, @Flags("other") @Optional target: User) {
        if (target == null) return
        if (!checkExistence(user, "invite")) return
        if (!hasPermission(user, Permissions.OPEN_OR_CLOSE, "invite")) return
        if (target.isOnArmy()) {
            if (target.getArmy() == user.getArmy()) {
                user.msgC("commands invite same-army")
                return
            }
        }
        Invite(user, target, user.getArmy())
        user.msgCR("commands invite sender-msg", user.getPlayer()!!.displayName, user.getArmy().name)
        target.msgCR("commands invite receiver-msg", user.getPlayer()!!.displayName, user.getArmy().name)

    }

    @Subcommand("name")
    @Description("returns the name of your army")
    fun name(user: User) {
        if (!checkExistence(user, "name")) return
        user.msgCR("commands name found", user.getArmy().name)
    }

    @Subcommand("perms")
    @Description("add or take a permission from a rank")
    fun perms(user: User) {
        if (!checkExistence(user, "perms")) return
        if (!hasPermission(user, Permissions.PERM, "perms")) return
        RankGui(user)

    }

    @Subcommand("shop")
    @Description("shop gui")
    fun shop(user: User) {
        if (!checkExistence(user, "shop")) return
        if (!hasPermission(user, Permissions.SHOP, "shop")) return
        ShopGui(user)
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

    }
}