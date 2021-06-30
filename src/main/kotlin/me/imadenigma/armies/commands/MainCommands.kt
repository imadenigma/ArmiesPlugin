@file:Suppress("SENSELESS_COMPARISON")

package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.guis.AdminGui
import me.imadenigma.armies.guis.MenuGui
import me.imadenigma.armies.guis.RankGui
import me.imadenigma.armies.user.Invite
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.utils.toWordFormat
import me.lucko.helper.Services
import me.lucko.helper.utils.Players
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import java.util.*

@CommandAlias("a|army")
class MainCommands : BaseCommand() {

    @Subcommand("create")
    @Syntax("<name>")
    fun create(user: User, name: String) {
        if (user.isOnArmy()) {
            user.msgC("commands create already-in-army")
            return
        }
        if (name == " " || name.contains("nazi", true) || name.length <= 3 || name.length >= 14) {
            user.msg("&4Name not valid")
            return
        }
        if (Army.armies.any { it.name.equals(name, true) || StringUtils.remove(it.name, ' ') == StringUtils.remove(name, ' ') }) {
            user.msg("&4Name taken")
            return
        }
        if (Army.getByLocation(user.player!!.location.x, user.player!!.location.z) != null ||
                Army.armies.any {
                    it.lands.any { land ->
                        for (x in (user.player!!.location.x - 16).toInt()..((user.player!!.location.x + 16).toInt())) {
                            for (z in (user.player!!.location.z - 16).toInt()..((user.player!!.location.z + 16).toInt())) {
                                if (Army.getByLocation(x.toDouble(), z.toDouble()) != null) return@any true
                            }
                        }
                        false
                    }
                }) {
            user.msg("&4You can't create an army in a claimed area !")
            return
        }
        success(user, "create")
        user.rank = Rank.EMPEROR
        val block = user.player!!.world.getBlockAt(user.player!!.location)
        block.type = Material.BEACON
        block.state.update()
        Army(UUID.randomUUID(), name, user.uuid, core = block, home = block.location)
    }

    @Subcommand("sethome")
    @Description("set home's location of the army")
    fun sethome(user: User) {
        checkExistence(user, "sethome")
        if (!hasPermission(user, Permissions.SET_HOME, "sethome")) return
        val army = Army.getByLocation(user.player!!.location.x, user.player!!.location.z) ?: kotlin.run {
            user.msg("&cYou must set your home in a claimed area by your army")
            return
        }
        if (army == user.army) {
            Army.armies.first { it.members.contains(user) }.home = user.player!!.location
            success(user, "sethome")
            return
        }
        user.msg("&cYou can't set your home in a claimed area by another army !")

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
        if (user.army.invades.isNotEmpty()) {
            user.msg("&4You can't leave your army while invading")
            return
        }
        success(user, "leave")
        user.army.kickMember(user)
    }

    @Subcommand("join")
    @CommandCompletion("@army")
    @Syntax("<army>")
    @Description("join an army")
    fun join(user: User, armyName: String) {
        val army = Army.armies.firstOrNull { it.name.equals(armyName, true) } ?: run {
            user.msg("&4Army not found !")
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
        user.army.isOpened = true
        success(user, "open")
    }

    @Subcommand("close")
    @Description("close your army")
    fun close(user: User) {
        if (!checkExistence(user, "close")) return
        if (!hasPermission(user, Permissions.OPEN_OR_CLOSE, "close")) return
        user.army.isOpened = false
        success(user, "close")
    }


    @Subcommand("name")
    @Description("returns the name of your army")
    fun name(user: User) {
        if (!checkExistence(user, "name")) return
        user.msgCR("commands name found", user.army.name)
    }

    @Subcommand("names")
    @Description("names of all armies")
    fun names(user: User) {
        user.msgC("commands names header")
        for (army in Army.armies) {
            val name = army.name
            val onlinePlayers = army.members.stream().filter { it.player != null }
                    .mapToInt { 1 }
                    .sum() + army.prisoners.stream().filter { it.player != null }
                    .mapToInt { 1 }
                    .sum()
            val total = army.members.size + army.prisoners.size
            user.msgCR("commands names format", name, onlinePlayers, total)
        }
    }

    @Subcommand("description")
    @Syntax("<description>")
    @Description("set the description of the army")
    fun description(user: User, description: String) {
        if (!checkExistence(user, "description")) return
        if (!hasPermission(user, Permissions.DESCRIPTION, "description")) return
        success(user, "description")
        user.army.description = description
    }

    @Subcommand("invite")
    @CommandCompletion("@user")
    @Syntax("<user>")
    @Description("invite your mate to join your army")
    fun invite(user: User, targetStr: String) {
        if (!checkExistence(user, "invite")) return
        if (!hasPermission(user, Permissions.INVITE, "invite")) return
        val target =
                User.users.filter { it.player != null }.firstOrNull { it.player!!.name.equals(targetStr, true) }
                        ?: kotlin.run {
                            user.msgCR("commands invite player-not-found", targetStr)
                            return
                        }
        if (user == target) {
            user.msg("&4You cannot invite yourself")
            return
        }

        if (target.isOnArmy()) {
            if (target.army == user.army) {
                user.msgC("commands invite same-army")
                return
            }
        }
        val invite = Invite(user, target, user.army)
        val msg = TextComponent("Click Here to accept")
        msg.color = ChatColor.GREEN
        msg.isBold = true
        msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/a join ${user.army.name}")

        invite.sender.msgCR(
                "commands invite sender-msg",
                invite.receiver.player!!.displayName,
                user.army.name
        )
        invite.receiver.msgCR(
                "commands invite receiver-msg",
                invite.sender.player!!.displayName,
                user.army.name
        )
        if (Players.getOffline(target.uuid).isPresent) target.player!!.spigot().sendMessage(msg)
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
    }

    @Subcommand("menu")
    @Description("open the menu gui")
    fun menu(user: User) {
        if (!checkExistence(user, "menu")) return
        if (!hasPermission(user, Permissions.MENU, "menu")) return
        MenuGui(user)
    }

    //                              //
    //         NEED TEST/FIX        //
    //                              //

    @Subcommand("promote")
    @Description("promote a member to a better rank")
    @Syntax("<target>")
    @CommandCompletion("@local_user")
    fun promote(user: User, targetName: String) {
        if (!checkExistence(user, "promote")) return
        if (!hasPermission(user, Permissions.PROMOTE, "promote")) return
        val target = User.users.stream()
                .filter { Objects.nonNull(it.player) }
                .filter { it.player!!.name.equals(targetName, true) }
                .findAny()
        if (!target.isPresent) {
            user.msg("&cTarget not found !")
            return
        }
        if (user.hasPermission(Permissions.PROMOTE)) {
            if (target.get().rank == Rank.EMPEROR) {
                user.msgC("commands promote emperor")
                return
            }
            if (target.get().rank == Rank.KNIGHT) {
                user.msgC("commands promote knight")
                return
            }
            if (target.get().rank == Rank.PRISONER) target.get().army.prisoners.remove(target.get())
            val nextRank = getNextRank(target.get().rank)
            target.get().rank = nextRank!!
            //here
            success(user, "promote", Players.getOffline(target.get().uuid).get().name, target.get().rank.name)
            target.get().msgCR("commands promote user-msg", user.player!!.displayName, target.get().rank.name)
        }
    }

    @Subcommand("demote")
    @Description("demote a member to a lower rank")
    @Syntax("<target>")
    @CommandCompletion("@local_user")
    fun demote(user: User, targetName: String) {
        if (!checkExistence(user, "demote")) return
        if (!hasPermission(user, Permissions.DEMOTE, "demote")) return
        val target = User.users.stream()
                .filter { Objects.nonNull(it.player) }
                .filter { it.player!!.name.equals(targetName, true) }
                .findAny()
        if (!target.isPresent()) {
            user.msg("&cTarget not found !")
            return
        }
        if (user.hasPermission(Permissions.DEMOTE)) {
            if (target.get().rank == Rank.EMPEROR) {
                user.msgC("commands demote emperor")
                return
            }
            val previousRank = getPreviousRank(target.get().rank)
            target.get().rank = previousRank
            //here
            success(user, "demote", Players.getOffline(target.get().uuid).get().name, target.get().rank.name)
            target.get().msgCR("commands demote user-msg", user.player!!.displayName, target.get().rank.name)
        }
    }

    @Subcommand("kick")
    @Description("kick a player")
    @Syntax("<target>")
    @CommandCompletion("@local_user")
    fun kick(user: User, targetName: String) {
        if (!checkExistence(user, "kick")) return
        if (!hasPermission(user, Permissions.KICK, "kick")) return
        val target = User.users.stream()
                .filter { Objects.nonNull(it.player) }
                .filter { it.player!!.name.equals(targetName, true) }
                .findAny()
        if (!target.isPresent) {
            user.msg("&cTarget not found !")
            return
        }
        user.army.kickMember(target.get())
        success(user, "kick", targetName)
        target.get().msgCR("commands kick user-msg", user.army)
    }

    @Subcommand("top")
    @Description("display top armies")
    fun top(user: User) {
        val conf = Services.load(Configuration::class.java).config.getNode("top-army")
        val size = conf.getNode("size").getInt(10)
        val line = conf.getNode("line").getString("").colorize()
        val title = conf.getNode("title").getString("")
        user.msg(title)
        val sortedArmies = Army.armies.sortedByDescending(Army::getBalance)
        sortedArmies.stream().limit(size.toLong()).forEachOrdered {
            val msg = line.replace("{0}", it.name).replace("{1}", it.getBalance().toString())
            user.msg(msg)
        }
    }

    @Subcommand("disband")
    @Description("disband your army")
    fun disband(user: User) {
        if (!checkExistence(user, "disband")) return
        if (!hasPermission(user, Permissions.DISBAND, "disband")) return
        if (user.army.invades.isNotEmpty()) {
            user.msg("&3You can't disband your army while invading or being invaded")
            return
        }
        user.army.msgC("army-msgs disband")
        user.army.members.forEach { it.rank = Rank.NOTHING }
        user.army.prisoners.forEach { it.rank = Rank.NOTHING }
        user.army.disband()
        success(user, "disband")
    }

    @Subcommand("chat")
    @Description("toggle army chat or toggle coalition chat")
    @Syntax("<on|off|a|c>")
    fun chat(user: User, type: String) {
        if (!checkExistence(user, "chat")) return
        if (type.equals("on", true) || type.equals("off", true)) {
            if (!hasPermission(user, Permissions.CHAT, "chat")) return
            user.armyChat = type.equals("on", true)
            if (user.armyChat) user.msg("&aYou enabled army chat")
            else user.msg("&cYou disabled army chat")
            return
        }
        if (type.equals("a", true) || type.equals("c", true)) {
            if (!hasPermission(user, Permissions.COALITION_CHAT, "chat coalition")) return
            if (type.equals("c", true)) {
                user.army.chatType = 'c'
                user.msgC("commands chat coalition enabled")
            } else {
                user.army.chatType = 'a'
                user.msgC("commands chat coalition disabled")
            }

        }
    }

    @Subcommand("admin")
    @Description("open up the admin gui for admins")
    @CommandPermission("army.admin")
    fun admin(user: User) {
        AdminGui(user)
    }

    @Subcommand("info|infos")
    @Description("get your army's informations")
    fun info(user: User) {
        if (!checkExistence(user, "info")) return
        val builder = StringBuilder()
        user.army.members.sortedBy { it.rank.ordinal }.stream().limit(5).forEachOrdered { builder.append(getPlayerName(it.uuid)).append("-") }
        builder.deleteCharAt(builder.length - 1)
        user.msg("&8&l&m============ <&r&3Infos of &6${user.army.name}&8&l&m> &l&m=============".colorize())
        user.msg("Description: &3${user.army.description}")
        user.msg("Leader: &6${getPlayerName(user.army.owner)}")
        user.msg("Treasury: &6${user.army.getBalance().toWordFormat()}")
        user.msg("Top-Members: &6$builder")
        user.msg("Created: &e${user.army.dateOfCreation.monthValue}/${user.army.dateOfCreation.dayOfMonth}/${user.army.dateOfCreation.year}")
        user.msg("Lands: &3${user.army.lands.size}")
        user.msg("Members (all): &3${user.army.members.size + user.army.prisoners.size}")
        user.msg("Members (online): &3${user.army.members.mapNotNull { it.player }.size + user.army.prisoners.mapNotNull { it.player }.size}")

    }


    companion object {

        fun getPreviousRank(rank: Rank): Rank {
            return when (rank) {
                Rank.EMPEROR -> Rank.KNIGHT
                Rank.KNIGHT -> Rank.SOLDIER
                Rank.SOLDIER -> Rank.PEASANT
                else -> Rank.PRISONER
            }
        }

        fun checkExistence(user: User, command: String): Boolean {
            if (!user.isOnArmy()) {
                user.msgC("commands $command not-in-army")
                return false
            }
            return true
        }

        fun success(user: User, command: String, vararg rep: Any) {
            user.msgCR("commands $command success", *rep)
        }

        fun hasPermission(user: User, permission: Permissions, command: String): Boolean {
            if (user.hasPermission(permission)) return true
            user.msgC("commands $command need-permission")
            return false
        }

        fun getPlayerName(uuid: UUID): String {
            return if (Players.getOffline(uuid).isPresent) Players.getOffline(uuid).get().name ?: "Console"
            else "Console"
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