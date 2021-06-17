package me.imadenigma.armies.army

import com.google.common.base.Preconditions
import com.google.gson.JsonElement
import me.imadenigma.armies.ArmyEconomy
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.exceptions.ArmyNotFoundException
import me.imadenigma.armies.user.Sender
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.asUUID
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.BlockPosition
import me.lucko.helper.serialize.Position
import me.lucko.helper.serialize.Region
import org.apache.commons.lang.StringUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import java.util.*

class Army(
    val uuid: UUID = UUID.randomUUID(),
    var name: String,
    val owner: UUID,
    val members: MutableSet<User> = mutableSetOf(User.getByUUID(owner)),
    val turrets: MutableSet<UUID> = mutableSetOf(),
    var isOpened: Boolean = false,
    var core: Block,
    var home: Location,
    val lands: MutableSet<Region> = mutableSetOf(),
    private var treasury: Double = 0.0,
    var hp: Int = 250,
    val enemies: MutableSet<Army> = mutableSetOf(),
    val allies: MutableSet<Army> = mutableSetOf(),
    val prisoners: MutableSet<User> = mutableSetOf(),
    val invades: MutableSet<Invade> = mutableSetOf(),
    var chatType: Char = 'a',  //either a or c; a = army, c = coalition
) : GsonSerializable, ArmyEconomy, Sender {
    var enemUUID = setOf<UUID>()
    var alliesUUID = setOf<UUID>()

    init {
        armies.add(this)
        val minLoc = this.core.location.subtract(8.0, 100.0, 8.0)
        val maxLoc = this.core.location.add(8.0, 100.0, 8.0)
        lands.add(Region.of(Position.of(minLoc), Position.of(maxLoc)))
    }

    fun claimArea(center: Location) {
        val minLoc = center.clone().subtract(8.0, 100.0, 8.0)
        val maxLoc = center.clone().add(8.0, 100.0, 8.0)
        lands.add(Region.of(Position.of(minLoc), Position.of(maxLoc)))
    }

    fun addMember(user: User) {
        if (user.isOnArmy()) {
            user.getArmy().kickMember(user)
        }
        user.armyChat = true
        user.rank = Rank.SOLDIER
        this.members.add(user)
    }

    fun kickMember(user: User) {
        this.members.remove(user)
        /*ser.rank = Rank.NOTHING
        user.armyChat = false
        if (this.members.size == 0) {
            this.members.forEach { it.rank = Rank.NOTHING }
            this.prisoners.forEach { it.rank = Rank.NOTHING }
            this.disband()
        }*/

    }

    fun takeDamage(damager: User) {
        if (this.invades.none { it.attacker == damager.getArmy().uuid || it.defender == damager.getArmy().uuid }) {
            if (damager.rank == Rank.EMPEROR || damager.rank == Rank.SOLDIER || damager.rank == Rank.KNIGHT) {
                val invade = Invade(damager.getArmy().uuid, this.uuid)
                this.invades.add(invade)
                damager.getArmy().invades.add(invade)
                val msg = Services.load(Configuration::class.java).config.getNode("invading-broadcast").getString("")
                Helper.server()
                    .broadcastMessage(msg.colorize().replace("{0}", damager.getArmy().name).replace("{1}", this.name))
            } else return
        }
        this.hp -= 5
        if (this.hp <= 0) {
            damager.getPlayer()!!.server.broadcastMessage("&aThe army &c${damager.getArmy().name} &awon the war against &c${this.name}".colorize())
        } else return
        this.core.type = Material.AIR
        this.core.state.update()
        if (damager.isOnArmy()) {
            for (invade in invades) {
                println("INVADING ...")
                if (invade.defender == this.uuid && invade.attacker == damager.getArmy().uuid) {
                    damager.getArmy().deposit(this.treasury * 0.7)
                    break
                }
                if (invade.attacker == this.uuid && invade.defender == damager.getArmy().uuid) {
                    damager.getArmy().deposit(this.treasury * 0.4)
                    break
                }
            }
            this.members.forEach {
                damager.getArmy().prisoners.add(it)
                it.rank = Rank.PRISONER
            }
            this.prisoners.forEach {
                damager.getArmy().prisoners.add(it)
                it.rank = Rank.PRISONER
            }
            this.turrets.forEach {
                damager.getArmy().turrets.add(it)
                Turrets.allTurrets.filter { turret -> turret.army == this }
                    .forEach { turret -> turret.army = damager.getArmy() }
            }
        }
        this.disband()
    }

    fun disband() {
        this.core.type = Material.AIR
        this.core.state.update()
        armies.stream().forEach { it1 ->
            val invade = it1.invades.stream().filter { it.defender == this.uuid || it.attacker == this.uuid }.findAny()
                .orElse(null)
            it1.invades.remove(invade)
            it1.enemies.remove(this)
            it1.allies.remove(this)
        }
        Turrets.allTurrets.filter { it.army == this }.forEach { it.despawn() }
        armies.remove(this)
        this.enemies.clear()
        this.invades.clear()
        this.members.clear()
        this.turrets.clear()
        this.name = ""
        this.isOpened = false
        this.prisoners.clear()
        this.lands.clear()
    }

    override fun serialize(): JsonElement {
        val jsMembers = JsonBuilder.array()
            .addAll(this.members.stream().map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
        val pos = BlockPosition.of(this.core).serialize()
        val home = Position.of(this.home).serialize()
        val enem = JsonBuilder.array().addAll(this.enemies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val alli = JsonBuilder.array().addAll(this.allies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val invades =
            JsonBuilder.array().addAll(this.invades.map { JsonBuilder.primitive("${it.attacker}??${it.defender}") })
        val areas = JsonBuilder.array().addAll(this.lands.map { it.serialize() })
        val turrets = JsonBuilder.array().addAll(this.turrets.map { JsonBuilder.primitive(it.toString()) })
        return JsonBuilder.`object`()
            .add("uuid", this.uuid.toString())
            .add("name", this.name)
            .add("owner", this.owner.toString())
            .add("members", jsMembers.build())
            .add("turrets", turrets.build())
            .add("isOpened", this.isOpened)
            .add("core", pos)
            .add("home", home)
            .add("areas", areas.build())
            .add("treasury", this.treasury)
            .add("hp", this.hp)
            .add("enemies", enem.build())
            .add("allies", alli.build())
            .add("chattype", this.chatType)
            .add("invades", invades.build())
            .add(
                "prisoners",
                JsonBuilder.array().addAll(this.prisoners.map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
                    .build()
            )
            .build()
    }

    override fun deposit(amount: Double) {
        Preconditions.checkArgument(amount >= 0, "Amount must be positive")
        this.treasury += amount
    }

    override fun withdraw(amount: Double) {
        Preconditions.checkArgument(amount >= 0, "Amount must be positive")
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
            val members = obj.get("members").asJsonArray.map { User.getByUUID(it.asUUID()) }.toMutableSet()
            val turrets = obj.get("turrets").asJsonArray.map { it.asUUID() }.toMutableSet()
            val isOpened = obj.get("isOpened").asBoolean
            val core = BlockPosition.deserialize(obj.get("core")).toBlock()
            val home = Position.deserialize(obj.get("home")).toLocation()
            val areas = obj.get("areas").asJsonArray.map { Region.deserialize(it) }.toMutableSet()
            val treasury = obj.get("treasury").asDouble
            val enemies = obj.get("enemies").asJsonArray.map { it.asUUID() }.toMutableSet()
            val allies = obj.get("allies").asJsonArray.map { it.asUUID() }.toMutableSet()
            val prisoners = obj.get("prisoners").asJsonArray.map { User.getByUUID(it.asUUID()) }.toMutableSet()
            val chatType = obj.get("chattype").asCharacter
            val invades = obj["invades"].asJsonArray.map {
                val str = it.asString
                val words = str.split("??")
                var attacker: UUID? = null
                var defender: UUID? = null
                kotlin.runCatching {
                    attacker = UUID.fromString(words[0])
                    defender = UUID.fromString(words[1])
                }
                return@map Invade(attacker!!, defender!!)
            }.toMutableSet()
            val hp = obj.get("hp").asInt
            val army =
                Army(
                    uuid,
                    name,
                    owner,
                    members,
                    turrets,
                    isOpened,
                    core,
                    home,
                    areas,
                    treasury,
                    hp,
                    prisoners = prisoners,
                    invades = invades,
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

        fun getByLocation(x: Double, z: Double): Army? {
            return armies.firstOrNull { it.lands.any { land -> land.inRegion(Position.of(x, 0.0, z,land.min.world)) } }
        }
    }

    override fun msgC(path: String) {
        val strs = StringUtils.split(path, " ")
        var node = User.language!!
        for (str in strs) node = node.getNode(str)
        val msg = node.getString("nullM")
        for (player in this.members.mapNotNull { it.getPlayer() }) {
            player.sendMessage(msg.colorize())
        }
    }

    override fun msgR(msg: String, vararg replacements: Any) {
        var s = msg
        for (i in replacements.indices) {
            s = s.replace("{$i}", replacements[i].toString())
        }
        for (player in this.members.mapNotNull { it.getPlayer() }) {
            player.sendMessage(s.colorize())
        }
    }

    override fun msgCR(path: String, vararg replacements: Any) {
        val strs = StringUtils.split(path, " ")
        var node = User.language!!
        for (str in strs) node = node.getNode(str)
        var msg = node.getString("nullM")
        for (i in replacements.indices) {
            msg = msg.replace("{$i}", replacements[i].toString())
        }
        for (player in this.members.mapNotNull { it.getPlayer() }) {
            player.sendMessage(msg.colorize())
        }
    }

    override fun msg(msg: String) {
        for (player in this.members.mapNotNull { it.getPlayer() }) {
            player.sendMessage(msg.colorize())
        }
    }

    fun privateMsg(msg: String) {
        for (member in this.members) {
            if (!member.armyChat) return
            member.msg(msg)
        }
    }

}

