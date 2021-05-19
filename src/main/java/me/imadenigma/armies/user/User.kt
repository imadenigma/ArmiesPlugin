package me.imadenigma.armies.user

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.colorize
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

class User(
    val offlinePlayer: OfflinePlayer,
    var rank: Rank = Rank.SOLDIER,
    val additionalPerms: MutableSet<Permissions> = mutableSetOf()
) : GsonSerializable, Sender {

    init {
        users.add(this)
        if (language == null) language = Services.load(Configuration::class.java).language
    }


    fun isOnArmy() : Boolean {
        return Army.armies.any { it.members.contains(this) }
    }



    override fun serialize(): JsonElement {
        val jsonArray = JsonArray()
        for (perm in additionalPerms) {
            jsonArray.add(perm.name)
        }
        return JsonBuilder.`object`()
            .add("uuid", this.offlinePlayer.uniqueId.toString())
            .add("rank", this.rank.name)
            .add("additionalPerms", jsonArray)
            .build()
    }


    override fun msgC(path: String) {
        if (!this.offlinePlayer.isOnline) return
        var strs = StringUtils.split(path," ")
        var node = language!!
        for (str in strs) node = node.getNode(str)
        this.offlinePlayer.player.sendMessage(node.getString("null").colorize())
    }

    override fun msgR(msg: String, vararg replacements: Any) {
        if (!this.offlinePlayer.isOnline) return
        if (!msg.contains("{")) return
        for (i in replacements.indices) {
            msg.replace("{$i}",replacements[i].toString())
        }
        this.offlinePlayer.player.sendMessage(msg.colorize())
    }


    override fun msgCR(path: String, vararg replacements: Any) {
        if (!this.offlinePlayer.isOnline) return
        var strs = StringUtils.split(path," ")
        var node = language!!
        for (str in strs) node = node.getNode(str)
        val msg = node.getString("null")
        for (i in replacements.indices) {
            msg.replace("{$i}",replacements[i].toString())
        }
        this.offlinePlayer.player.sendMessage(msg.colorize())
    }

    companion object {

        val users = mutableSetOf<User>()
        var language: ConfigurationNode? = null

        fun deserialize(jsonElement: JsonElement): User {
            val obj = jsonElement.asJsonObject
            val uuid = UUID.fromString(obj.get("uuid").asString)
            val rank = Rank.valueOf(obj.get("rank").asString)
            val addiPerms =
                obj.get("additionalPerms").asJsonArray.map { Permissions.valueOf(it.asString) }.toMutableSet()
            return User(Bukkit.getOfflinePlayer(uuid), rank, addiPerms)
        }

        fun getByUUID(uuid: UUID): User {
            return this.users.stream().filter { it.offlinePlayer.uniqueId == uuid }.findAny()
                .orElse(User(Bukkit.getOfflinePlayer(uuid)))
        }

    }
}