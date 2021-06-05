package me.imadenigma.armies.weapons

import com.google.gson.JsonElement
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.sentryGun.Sentry0
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
    val damage: Double,
    val distance: Double,
    val uuid: UUID
) : GsonSerializable {

    abstract override fun serialize(): JsonElement

    abstract fun spawn()
    abstract fun despawn()
    abstract fun create(user: User)
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
            .build()
    }


    companion object {
        val allTurrets = mutableSetOf<Turrets>()
        fun deserialize(jsonElement: JsonElement) {
            val obj = jsonElement.asJsonObject
            val location = Position.deserialize(obj["position"]).toLocation()
            val army = Army.getByUUID(UUID.fromString(obj["army"].asString))
            val ammo = obj.get("ammo").asInt
            val type = obj.get("type").asString
            if (type == "sentry-0") {
                Sentry0(location,army,ammo).spawn()
            }
        }
    }
}