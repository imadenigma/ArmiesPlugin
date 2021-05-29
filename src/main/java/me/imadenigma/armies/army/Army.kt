package me.imadenigma.armies.army

import com.google.gson.JsonElement
import me.imadenigma.armies.ArmyEconomy
import me.imadenigma.armies.exceptions.ArmyNotFoundException
import me.imadenigma.armies.toUUID
import me.imadenigma.armies.user.User
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.BlockPosition
import me.lucko.helper.serialize.ChunkPosition
import me.lucko.helper.serialize.Position
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.*

class Army(
    val uuid: UUID = UUID.randomUUID(),
    var name: String,
    val owner: UUID,
    val members: MutableSet<User> = mutableSetOf(User.getByUUID(owner)),
    var isOpened: Boolean = false,
    var core: Block,
    var home: Location,
    val lands: MutableSet<ChunkPosition> = mutableSetOf(),
    private var treasury: Double = 0.0,
    var hp: Int = 250,
    val enemies: MutableSet<Army> = mutableSetOf(),
    val allies: MutableSet<Army> = mutableSetOf(),
    val prisoners: MutableSet<User> = mutableSetOf(),
    val chatType: Char = 'a'  //either a or c; a = army, c = coalition
) : GsonSerializable, ArmyEconomy {
    var enemUUID = setOf<UUID>()
    var alliesUUID = setOf<UUID>()

    init {
        armies.add(this)
        lands.add(ChunkPosition.of(this.home.chunk))
    }

    fun addMember(user: User) {
        if (user.rank != Rank.NOTHING) {
            user.getArmy().kickMember(user)
        }
        user.rank = Rank.TROOPS
        this.members.add(user)
    }

    fun kickMember(user: User) {
        this.members.remove(user)
        user.rank = Rank.NOTHING
    }

    override fun serialize(): JsonElement {
        val jsMembers = JsonBuilder.array()
            .addAll(this.members.stream().map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
        val pos = BlockPosition.of(this.core).serialize()
        val home = Position.of(this.home).serialize()
        val enem = JsonBuilder.array().addAll(this.enemies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val alli = JsonBuilder.array().addAll(this.allies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val areas = JsonBuilder.array().addAll(this.lands.map { it.serialize() })
        return JsonBuilder.`object`()
            .add("uuid", this.uuid.toString())
            .add("name", this.name)
            .add("owner", this.owner.toString())
            .add("members", jsMembers.build())
            .add("isOpened", this.isOpened)
            .add("core", pos)
            .add("home",home)
            .add("areas",areas.build())
            .add("treasury", this.treasury)
            .add("hp",this.hp)
            .add("enemies", enem.build())
            .add("allies", alli.build())
            .add("chattype", this.chatType)
            .add(
                "prisoners",
                JsonBuilder.array().addAll(this.prisoners.map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
                    .build()
            )
            .build()
    }

    override fun deposit(amount: Double) {
        this.treasury += amount
    }

    override fun withdraw(amount: Double) {
        if (this.treasury - amount >= 0) {
            this.treasury -= amount
            return
        }
        this.treasury = 0.0
    }

    override fun getBalance(): Double {
        return this.treasury
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
            val core = BlockPosition.deserialize(obj.get("core")).toBlock()
            val home = Position.deserialize(obj.get("home")).toLocation()
            val areas = obj.get("areas").asJsonArray.map { ChunkPosition.deserialize(it) }.toMutableSet()
            val treasury = obj.get("treasury").asDouble
            val enemies = obj.get("enemies").asJsonArray.map { it.toUUID() }.toSet()
            val allies = obj.get("allies").asJsonArray.map { it.toUUID() }.toSet()
            val prisoners = obj.get("prisoners").asJsonArray.map { User.getByUUID(it.toUUID()) }.toMutableSet()
            val chatType = obj.get("chattype").asCharacter
            val hp = obj.get("hp").asInt
            val army =
                Army(
                    uuid,
                    name,
                    owner,
                    members,
                    isOpened,
                    core,
                    home,
                    areas,
                    treasury,
                    hp,
                    prisoners = prisoners,
                    chatType = chatType
                )
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