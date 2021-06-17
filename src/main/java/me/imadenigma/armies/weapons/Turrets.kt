package me.imadenigma.armies.weapons

import com.google.gson.JsonElement
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.HologramBuilder
import me.imadenigma.armies.utils.asUUID
import me.imadenigma.armies.weapons.impl.FireballTurret
import me.imadenigma.armies.weapons.impl.ManualFireTurret
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.Position
import org.apache.commons.codec.binary.Base64.encodeBase64
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SkullType
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.boss.BossBar
import java.util.*
import kotlin.math.roundToInt


abstract class Turrets(
    val name: String,
    open val location: Location,
    open var army: Army?,
    open var ammo: Int,
    open var hp: Double,
    open var damage: Double,
    open var distance: Double,
    open var level: Int,
    open val uuid: UUID
) : GsonSerializable {

    abstract val bossbar: BossBar

    abstract override fun serialize(): JsonElement

    abstract fun spawn(): Boolean
    abstract fun despawn()
    abstract fun addAmmo(user: User, amount: Int)
    abstract fun function()
    abstract fun registerListeners()

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
        }
        else if (this is FireballTurret) {
            when (this.level) {
                0 -> {
                    this.damage += 3; this.distance = 16.0; this.level++; this.hp += 50
                }
                1 -> {
                    this.damage += 5; this.distance = 32.0; this.level++; this.hp += 50
                }
                2 -> {
                    this.damage += 7; this.distance = 48.0; this.level++; this.hp += 100
                }
                else -> upgrader.msg("&4there is no more levels, this turret's level is the max")
            }
        }
        else if (this is ManualFireTurret && this.level == 1) {
            this.level++
            this.hp += 50
        }
        HologramBuilder.updateBlockName(
            this.location.block.location.add(0.0, 1.0, 0.0).block,
            this.name + " $level"
        )
    }

    fun serialise(type: String): JsonElement {
        this.despawn()
        val pos = Position.of(this.location)
        val army = this.army?.uuid.toString()
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

    open fun setSkullBlock(locBloque: Block, url: String) {
        locBloque.type = Material.SKULL
        val skullBlock = locBloque.state as Skull
        skullBlock.skullType = SkullType.PLAYER
        val profile = GameProfile(UUID.randomUUID(), null)
        val encodedData = encodeBase64(java.lang.String.format("{textures:{SKIN:{url:\"%s\"}}}", url).toByteArray())
        profile.properties.put("textures", Property("textures", String(encodedData)))
        try {
            val profileField = skullBlock.javaClass.getDeclaredField("profile")
            profileField.isAccessible = true
            profileField[skullBlock] = profile
        } catch (var7: IllegalAccessException) {
            var7.printStackTrace()
        } catch (var7: NoSuchFieldException) {
            var7.printStackTrace()
        }
        skullBlock.update()
    }



    abstract fun takeDamage(user: User?)



    companion object {
        private val axis = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
        private val radial = arrayOf(
            BlockFace.NORTH,
            BlockFace.NORTH_EAST,
            BlockFace.EAST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH,
            BlockFace.SOUTH_WEST,
            BlockFace.WEST,
            BlockFace.NORTH_WEST
        )

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
                when (type) {
                    "sentry" -> Sentry(location, army, ammo, level, hp, damage, distance, uuid)
                    "manual-gun" -> ManualFireTurret(location, ammo, level, uuid)
                    else -> FireballTurret(location, army, ammo, level, hp, damage, distance, uuid)
                }
            }
        }
        fun yawToFace(yaw: Float, useSubCardinalDirections: Boolean): BlockFace {
            return if (useSubCardinalDirections) radial[(yaw / 45f).roundToInt() and 0x7].oppositeFace else axis[(yaw / 90f).roundToInt() and 0x3].oppositeFace
        }
    }
}