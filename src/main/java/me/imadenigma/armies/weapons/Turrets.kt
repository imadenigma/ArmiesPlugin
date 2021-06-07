package me.imadenigma.armies.weapons

import com.google.gson.JsonElement
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.utils.asUUID
import me.imadenigma.armies.utils.getClaimCard
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.Position
import org.bukkit.Location
import java.util.*

abstract class Turrets(
    val name: String,
    open val location: Location,
    open val army: Army,
    open var ammo: Int,
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
            .add("uuid", this.uuid.toString())
            .build()
    }


    fun create(user: User) {
        this.spawn()
        user.getArmy().turrets.add(this.uuid)
    }

    fun upgrade(upgrader: User) {
        upgrader.getPlayer()!!.inventory.remove(getClaimCard())
        if (this is Sentry) {
            when (this.level) {
                1 -> { this.damage = 7.0; this.distance = 16.0; this.level++ }
                2 -> { this.damage = 9.0; this.distance = 32.0; this.level++ }
                else -> upgrader.msg("&4there is no more levels, this turret's level is the max")
            }

        }
    }

    companion object {
        val allTurrets = mutableSetOf<Turrets>()
        fun deserialize(jsonElement: JsonElement) {
            val obj = jsonElement.asJsonObject
            val location = Position.deserialize(obj["position"]).toLocation()
            val army = Army.getByUUID(obj.get("army").asUUID())
            val uuid = obj.get("uuid").asUUID()
            val ammo = obj.get("ammo").asInt
            val type = obj.get("type").asString
            if (type == "sentry-0") {
                Sentry(location,army,ammo, uuid = uuid)
            }
        }
    }
}