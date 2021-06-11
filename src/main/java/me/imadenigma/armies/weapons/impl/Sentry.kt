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
import org.bukkit.block.BlockFace
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import java.util.*
import java.util.concurrent.TimeUnit


class Sentry(
    override val location: Location,
    override val army: Army,
    override var ammo: Int = 100,
    override var level: Int = 0,
    override var hp: Double = 50.0,
    override var damage: Double = 5.0,
    override var distance: Double = 8.0,
    override val uuid: UUID
) : Turrets("Sentry Turret", location, army, ammo, hp, damage, distance, level, uuid) {

    private var isEnabled = true

    init {
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.sync().runRepeating(this::function, 2L, 7L)
            Schedulers.async().runRepeating({ task ->
                if (this.hp <= 0.0) {
                    task.close()
                }
                this.isEnabled = true
            }, 30L, 30L)
        }
    }

    override fun spawn(): Boolean {
        val block = this.location.block
        if (army.core == block) {
            this.location.add(1.0, 0.0, 0.0)
            return spawn()
        }
        block.type = Material.REDSTONE_BLOCK
        block.state.update()
        val block2 = block.location.add(0.0, 1.0, 0.0).block
        block2.type = Material.SKULL
        block2.state.update()
        Metadata.provideForBlock(block).put(MetadataKeys.UNBREAKABLE, true)
        Metadata.provideForBlock(block2).put(MetadataKeys.UNBREAKABLE, true)
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
        army.turrets.remove(this.uuid)
        allTurrets.remove(this)
        HologramBuilder.removeBlockName(this.location.add(0.0, 1.0, 0.0).block)
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
        if (this.ammo <= 0) return
        if (!this.isEnabled) return
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
        val p1 = entity.location.toVector()
        val loc = this.location.clone().add(0.0, 2.3, 0.0)
        val p2 = this.location.toVector()
        val vec = p1.clone().subtract(p2).normalize()
        val dst = loc.distance(entity.location)
        this.isEnabled = false
        val speed =
            when {
                dst < 6.5 -> 0.6F
                dst < 15 -> 1F
                else -> 1.5F
            }
        val arrow = this.location.world.spawnArrow(loc, vec, speed, 12F)
        arrow.pickupStatus = Arrow.PickupStatus.DISALLOWED
        Metadata.provideForEntity(arrow).put(MetadataKeys.SENTRY, true)
    }

    override fun registerListeners() {
        Events.subscribe(ProjectileHitEvent::class.java)
            .filter { it.entity is Arrow }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.SENTRY].isPresent }
            .handler {
                Schedulers.sync().runLater({
                    if ((it.entity as Arrow).isInBlock) it.entity.remove()
                }, 2L, TimeUnit.SECONDS)
            }
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Arrow }
            .filter { it.entity is Player }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.SENTRY].isPresent }
            .handler {
                it.damage = this.damage
                Schedulers.sync().runLater({
                    Metadata.provideForEntity(it.entity).remove(MetadataKeys.SENTRY)
                }, 2L, TimeUnit.SECONDS)
            }
    }

    override fun serialize(): JsonElement {
        return serialise("sentry-$level")
    }


}