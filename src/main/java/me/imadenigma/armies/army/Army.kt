package me.imadenigma.armies.army

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import me.imadenigma.armies.exceptions.ArmyNotFoundException
import me.imadenigma.armies.toUUID
import me.imadenigma.armies.user.User
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.Position
import org.bukkit.Location
import java.util.*

class Army(
    val uuid: UUID = UUID.randomUUID(),
    var name: String,
    val owner: UUID,
    val members: MutableSet<User> = mutableSetOf(User.getByUUID(owner)),
    var isOpened: Boolean = false,
    var home: Location? = null,
    var treasury: Double = 0.0,
    val enemies: MutableSet<Army> = mutableSetOf(),
    val allies: MutableSet<Army> = mutableSetOf()
) : GsonSerializable {
    var enemUUID = setOf<UUID>()
    var alliesUUID = setOf<UUID>()

    init {
        armies.add(this)
    }


    override fun serialize(): JsonElement {
        val jsMembers = JsonBuilder.array()
            .addAll(this.members.stream().map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
        var pos: JsonElement? = null
        if (this.home != null) pos = Position.of(this.home).serialize()
        val enem = JsonBuilder.array().addAll(this.enemies.stream().map { JsonBuilder.primitive(it.uuid.toString()) })
        val alli = JsonBuilder.array().addAll(this.allies.stream().map { JsonBuilder.primitive(it.uuid.toString()) })

        return JsonBuilder.`object`()
            .add("uuid", this.uuid.toString())
            .add("name", this.name)
            .add("owner", this.owner.toString())
            .add("members", jsMembers.build())
            .add("isOpened", this.isOpened)
            .add("home", pos)
            .add("treasury", this.treasury)
            .add("enemies", enem.build())
            .add("allies", alli.build())
            .build()
    }

    companion object {

        val armies = mutableSetOf<Army>()

        fun deserialize(jsonElement: JsonElement): Army {
            val obj = jsonElement.asJsonObject
            val uuid = UUID.fromString(obj.get("uuid").asString)
            val name = obj.get("name").asString
            val owner = UUID.fromString(obj.get("owner").asString)
            val members = obj.get("members").asJsonArray.map { User.getByUUID(it.toUUID()) }.toMutableSet()
            val isOpened = obj.get("isOpened").asBoolean
            var home: Location? = null
            if (obj.get("home") != null)  home = Position.deserialize(obj.get("home")).toLocation()
            val treasury = obj.get("treasury").asDouble
            val enemies = obj.get("enemies").asJsonArray.map { it.toUUID() }.toSet()
            val allies = obj.get("allies").asJsonArray.map { it.toUUID() }.toSet()

            val army = Army(uuid, name, owner, members, isOpened, home, treasury)
            army.enemUUID = enemies
            army.alliesUUID = allies
            return army
        }

        fun getByUUID(uuid: UUID): Army {
            return this.armies.stream().filter { it.uuid == uuid }.findAny()
                .orElseThrow { ArmyNotFoundException(uuid.toString()) }
        }
    }
}