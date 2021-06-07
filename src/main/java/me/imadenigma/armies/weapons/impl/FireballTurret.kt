package me.imadenigma.armies.weapons.impl

import com.google.gson.JsonElement
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.HologramBuilder
import me.imadenigma.armies.utils.MetadataKeys
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.metadata.Metadata
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SkullType
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.material.Torch
import java.util.*

class FireballTurret(
    override val location: Location,
    override val army: Army,
    override var ammo: Int = 100,
    override var level: Int = 1,
    override var damage: Double = 5.0,
    override var distance: Double = 8.0,
    override val uuid: UUID = UUID.randomUUID()
) : Turrets("Gun Turret", location, army, ammo, damage, distance, level, uuid) {

    init {
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.sync().runRepeating(this::function, 2L, 7L)
        }
    }


    override fun serialize(): JsonElement {
        return serialise("gun-$level")
    }

    override fun spawn(): Boolean {
        val block = this.location.block
        if (army.core == block) return false
        block.type = Material.OBSIDIAN
        block.state.update()
        val block2 = block.location.add(0.0, 1.0, 0.0).block
        if (army.core == block2) return false
        block2.type = Material.END_ROD
        block2.state.update()
        block2.world.spawnFallingBlock(block2.location, block2.state.data)
        HologramBuilder.updateBlockName(block2, this.name + " $level")
        return true
    }

    override fun despawn() {
        val block = this.location.block
        block.type = Material.AIR
        block.state.update()
        val block2 = block.getRelative(BlockFace.UP)
        block2.type = Material.AIR
        block2.state.update()
    }

    override fun addAmmo(user: User, amount: Int) {
        for (content in user.getPlayer()!!.inventory.contents) {
            content ?: continue
            if (content.type == Material.IRON_NUGGET) {
                with(user) {
                    getPlayer()!!.inventory.remove(content)
                }
                this.ammo += amount
                return
            }
        }
    }

    override fun function() {
        val bool = HologramBuilder.updateBlockName(this.location.block.location.add(0.0, 1.0, 0.0).block, this.name + " $level")
        if (!bool) return
        if (this.ammo <= 0) return
        val entity = this.location.world
            .getNearbyEntities(this.location, this.distance, this.distance, this.distance)
            .filterIsInstance(Player::class.java)
            .filterNot {
                val user = User.getByUUID(it.uniqueId)
                if (!user.isOnArmy()) return@filterNot false
                else return@filterNot if (user.getArmy() == this.army) return@filterNot true
                else false
            }
            .firstOrNull() ?: return
        this.ammo--
        val p1 = entity.location.subtract(0.0, 1.0, 0.0).toVector()
        val loc = this.location.clone().add(0.0, 2.3, 0.0)
        val p2 = this.location.toVector()
        val vec = p1.clone().subtract(p2)
        this.location.world.spawn(loc, Fireball::class.java) {
            Metadata.provideForEntity(it).put(MetadataKeys.SENTRY, true)
            it.direction = vec.multiply(level * 7)
            it.yield = when(level) {
                1 -> 6F
                2 -> 8F
                else -> 10F
            }
        }
    }

    override fun registerListeners() {
        Events.subscribe(ExplosionPrimeEvent::class.java)
            .filter { it.entity is Fireball }
            .filter { Metadata.provideForEntity(it.entity).has(MetadataKeys.GUN) }
            .handler {
                it.fire = true
                it.isCancelled = true
            }
    }
}