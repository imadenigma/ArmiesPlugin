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
import org.bukkit.entity.Explosive
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import java.util.*

class FireballTurret(
    override val location: Location,
    override val army: Army,
    override var ammo: Int = 100,
    override var level: Int = 0,
    override var hp: Double = 50.0,
    override var damage: Double = 5.0,
    override var distance: Double = 8.0,
    override val uuid: UUID = UUID.randomUUID()
) : Turrets("Gun Turret", location, army, ammo, hp, damage, distance, level, uuid) {

    private var isEnabled: Boolean = true

    init {
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.sync().runRepeating(this::function, 2L, 7L)
            Schedulers.async().runRepeating({ _ -> this.isEnabled = true }, 30L, 30L)
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
        block2.type = Material.BLACK_GLAZED_TERRACOTTA
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
        if (!isEnabled)
            return
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
        this.isEnabled = false
        this.location.world.spawn(loc, Fireball::class.java) {
            Metadata.provideForEntity(it).put(MetadataKeys.GUN, true)
            it.direction = vec.multiply(level * 7)
            it.yield = when (level) {
                1 -> 6F
                2 -> 8F
                else -> 10F
            }
            it.setIsIncendiary(false)
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
        Events.subscribe(EntityExplodeEvent::class.java, EventPriority.HIGHEST)
            .filter { it.entity is Fireball }
            .filter { Metadata.provideForEntity(it.entity).has(MetadataKeys.GUN) }
            .handler {
                it.blockList().clear()
            }
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Fireball }
            .filter { Metadata.provideForEntity(it.damager).has(MetadataKeys.GUN) }
            .filter { it.entity is Player }
            .handler {
                (it.entity as Player).damage(this.damage)
            }
    }
}