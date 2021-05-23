package me.imadenigma.armies.user

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import me.imadenigma.armies.Armies
import me.imadenigma.armies.ArmyEconomy
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.colorize
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.utils.Log
import me.lucko.helper.utils.Players
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashSet

class User(
    val uuid: UUID,
    var rank: Rank = Rank.SOLDIER,
    private val additionalPerms: MutableSet<Permissions> = mutableSetOf()
) : GsonSerializable, Sender, ArmyEconomy {

    init {
        users.add(this)
        Log.info("registering a new user: ${this.uuid}")
        if (language == null) language = Services.load(Configuration::class.java).language
    }


    fun isOnArmy() : Boolean {
        return Army.armies.any { it.members.contains(this) }
    }

    fun getArmy() : Army {
        return Army.armies.first { it.members.contains(this) }
    }

    fun getPlayer() : Player {
        return Bukkit.getPlayer(this.uuid)
    }

    override fun serialize(): JsonElement {
        val jsonArray = JsonArray()
        for (perm in additionalPerms) {
            jsonArray.add(perm.name)
        }
        return JsonBuilder.`object`()
            .add("uuid", this.uuid.toString())
            .add("rank", this.rank.name)
            .add("additionalPerms", jsonArray)
            .build()
    }


    override fun msgC(path: String) {
        val player = Bukkit.getPlayer(this.uuid)
        val strs = StringUtils.split(path," ")
        var node = language!!
        for (str in strs) node = node.getNode(str)
        player.sendMessage(node.getString("null").colorize())
    }

    override fun msgR(msg: String, vararg replacements: Any) {
        val player = Bukkit.getPlayer(this.uuid)
        if (!msg.contains("{")) return
        for (i in replacements.indices) {
            msg.replace("{$i}",replacements[i].toString())
        }
        player.sendMessage(msg.colorize())
    }


    override fun msgCR(path: String, vararg replacements: Any) {
        val player = Bukkit.getPlayer(this.uuid)
        val strs = StringUtils.split(path," ")
        var node = language!!
        for (str in strs) node = node.getNode(str)
        var msg = node.getString("null")
        for (i in replacements.indices) {
            msg = StringUtils.replace(msg,"{$i}",replacements[i].toString())
        }
        player.sendMessage(msg.colorize())
    }

    override fun msg(msg: String) {
        this.getPlayer().sendMessage(msg.colorize())
    }

    companion object {

        val users = HashSet<User>()
        var language: ConfigurationNode? = null

        fun deserialize(jsonElement: JsonElement): User {
            val obj = jsonElement.asJsonObject
            val uuid = UUID.fromString(obj.get("uuid").asString)
            val rank = Rank.valueOf(obj.get("rank").asString)
            val addiPerms =
                obj.get("additionalPerms").asJsonArray.map { Permissions.valueOf(it.asString) }.toMutableSet()
            return User(uuid, rank, addiPerms)
        }

        fun getByUUID(uuid: UUID): User {
            return this.users.firstOrNull { it.uuid.equals(uuid) } ?: User(uuid)
        }

    }

    override fun deposit(amount: Double) {
        val econ = Services.load(Armies::class.java).econ!!
        econ.depositPlayer(Players.getOffline(this.uuid).get(),amount)
    }

    override fun withdraw(amount: Double) {
        val econ = Services.load(Armies::class.java).econ!!
        econ.withdrawPlayer(Players.getOffline(this.uuid).get(),amount)
    }

    override fun getBalance(): Double {
        val econ = Services.load(Armies::class.java).econ!!
        return econ.getBalance(Players.getOffline(this.uuid).get())
    }
}