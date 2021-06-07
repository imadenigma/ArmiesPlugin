package me.imadenigma.armies.weapons

import com.google.gson.JsonElement
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.utils.asUUID
import me.imadenigma.armies.utils.getClaimCard
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.HologramBuilder
import me.imadenigma.armies.weapons.impl.FireballTurret
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.Position
import org.bukkit.Location
import java.util.*
import kotlin.concurrent.timer
import kotlin.concurrent.timerTask

abstract class Turrets(
    val name: String,
    open val location: Location,
    open val army: Army,
    open var ammo: Int,
    open var hp: Double,
    open var damage: Double,
    open var distance: Double,
    open var level: Int,
    open val uuid: UUID
) : GsonSerializable {

    abstract override fun serialize(): JsonElement

    abstract fun spawn(): Boolean
    abstract fun despawn()
    abstract fun addAmmo(user: User, amount: Int)
    abstract fun function()
    abstract fun registerListeners()

    fun create(user: User) {
        this.spawn()
        user.getArmy().turrets.add(this.uuid)
    }

    fun upgrade(upgrader: User) {
        if (this is Sentry) {
            when (this.level) {
                0 -> {
                    this.damage = 7.0; this.distance = 16.0; this.level++; this.hp = 100.0
                }
                1 -> {
                    this.damage = 9.0; this.distance = 32.0; this.level++; this.hp = 250.0
                }
                else -> upgrader.msg("&4there is no more levels, this turret's level is the max")
            }
        } else if (this is FireballTurret) {
            when (this.level) {
                0 -> {
                    this.damage += 3; this.distance = 16.0; this.level++; this.hp += 50
                }
                1 -> {
                    this.damage += 5; this.distance = 32.0; this.level++; this.hp += 50
                }
                2 -> {
                    this.damage += 7; this.distance = 48.0; this.level++; this.hp += 150
                }
                else -> upgrader.msg("&4there is no more levels, this turret's level is the max")
            }
        }
        HologramBuilder.updateBlockName(
            this.location.block.location.add(0.0, 2.0, 0.0).block,
            this.name + " $level"
        )
    }

    fun serialise(type: String): JsonElement {
        this.despawn()
        val pos = Position.of(this.location)
        val army = army.uuid.toString()
        val ammo = this.ammo
        return JsonBuilder.`object`()
            .add("position", pos)
            .add("army", army)
            .add("ammo", ammo)
            .add("type", type)
            .add("level", level)
            .add("damage", damage).add("hp", hp)
            .add("distance", distance)
            .add("uuid", this.uuid.toString())
            .build()
    }

    companion object {
        val allTurrets = mutableSetOf<Turrets>()
        fun deserialize(jsonElement: JsonElement) {
            val obj = jsonElement.asJsonObject
            obj.let {
                val location = Position.deserialize(it["position"]).toLocation()
                val army = Army.getByUUID(it["army"].asUUID())
                val uuid = it["uuid"].asUUID()
                val ammo = it["ammo"].asInt
                val type = it["type"].asString
                val level = it["level"].asInt
                val damage = it["damage"].asDouble
                val hp = it["hp"].asDouble
                val distance = it["distance"].asDouble
                if (type == "sentry") {
                    Sentry(location, army, ammo, level, hp, damage, distance, uuid)
                }else FireballTurret(location, army, ammo, level, hp, damage, distance, uuid)
            }

        }
    }
}