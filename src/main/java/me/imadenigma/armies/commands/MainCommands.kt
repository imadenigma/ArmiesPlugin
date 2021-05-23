package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.google.common.base.Preconditions
import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.user.Invite
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import org.bukkit.Bukkit
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
        Army(UUID.randomUUID(), name, user.uuid)
    }


    @Subcommand("sethome")
    @Description("set home's location of the army")
    fun sethome(user: User) {
        checkExistence(user, "sethome")
        if (user.rank != Rank.EMPEROR) {
            user.msgC("commands sethome not-emperor")
            return
        }
        Army.armies.first { it.owner == user.uuid }.home = user.getPlayer().location
        success(user, "sethome")
    }

    @Subcommand("home")
    @Description("teleport to your army's home")
    fun home(user: User) {
        if (!checkExistence(user, "home")) return
        if (checkIfPrisoner(user, "home")) return
        val army = Army.armies.first { it.members.contains(user) }
        if (army.home == null) {
            user.msgC("commands home dont-exists")
            return
        }
        val player = Bukkit.getPlayer(user.uuid)
        player.teleport(army.home)
        success(user, "home")
    }

    @Default
    @Subcommand("help")
    @CatchUnknown
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
        if (!checkIfPrisoner(user, "leave")) return
        success(user, "leave")
        user.rank = Rank.NOTHING
        Army.armies.first { it.members.contains(user) }.members.remove(user)
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
    fun promote(user: User, target: User) {
        if (user.rank != Rank.EMPEROR && user.rank != Rank.KNIGHT) {
            user.msgC("need-permission")
            return
        }
        if (!checkExistence(user, "promote")) return
        var index = Rank.sorted.indexOf(target.rank) + 1
        if (index == Rank.sorted.size || index == Rank.sorted.size - 1) index = Rank.sorted.size - 2
        target.rank = Rank.sorted[index]
        success(user, "promote", target.getPlayer().displayName, target.rank.name)
        user.msgCR("commands promote user-msg", user.getPlayer().displayName, target.rank)
    }


    @Subcommand("open")
    @Description("open your army to make players can join")
    fun open(user: User) {
        if (!checkExistence(user, "open")) return
        if (user.rank == Rank.KNIGHT || user.rank == Rank.EMPEROR) {
            user.getArmy().isOpened = true
            success(user, "open")
        }else user.msgC("commands open need-permission")
    }

    @Subcommand("close")
    @Description("close your army")
    fun close(user: User) {
        if (!checkExistence(user, "close")) return
        if (user.rank == Rank.KNIGHT || user.rank == Rank.EMPEROR) {
            user.getArmy().isOpened = false
            success(user, "close")
        }else user.msgC("commands close need-permission")
    }

    companion object {

        fun checkExistence(user: User, command: String): Boolean {
            if (user.rank == Rank.NOTHING) {
                user.msgC("commands $command not-in-army")
                return false
            }
            return true
        }

        fun checkIfPrisoner(user: User, command: String): Boolean {
            if (user.rank == Rank.PRISONER) {
                user.msgC("commands $command prisoner")
                return true
            }
            return false
        }

        fun success(user: User, command: String, vararg rep: Any) {
            user.msgCR("commands $command success", rep)
        }


    }
}