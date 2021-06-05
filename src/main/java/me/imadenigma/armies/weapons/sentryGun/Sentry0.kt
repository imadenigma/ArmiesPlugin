package me.imadenigma.armies.weapons.sentryGun

import com.google.gson.JsonElement
import me.imadenigma.armies.MetadataKeys
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.metadata.Metadata
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import java.util.*
import java.util.concurrent.TimeUnit


class Sentry0(
    override val location: Location,
    override val army: Army,
    override var ammo: Int = 100,
) : Turrets("Gun Turret", location, army, ammo, 7.0, 8.0, UUID.randomUUID()) {


    init {
        this.spawn()
        allTurrets.add(this)
        this.registerListeners()
        Schedulers.sync().runRepeating(this::function, 2L, 7L)
    }


    override fun spawn() {
        val block = this.location.block
        if (army.core == block) return
        block.type = Material.REDSTONE_BLOCK
        block.state.update()
        val block2 = block.location.add(0.0, 1.0, 0.0).block
        if (army.core == block2) return

        block2.type = Material.SKULL
        block2.state.update()
    }

    override fun despawn() {
        val block = this.location.block
        block.type = Material.AIR
        block.state.update()
        val block2 = block.getRelative(BlockFace.UP)
        block2.type = Material.AIR
        block2.state.update()
    }

    override fun create(user: User) {
        this.spawn()
        user.getArmy().turrets.add(this.uuid)
    }

    override fun addAmmo(user: User, amount: Int) {
        for (content in user.getPlayer().inventory.contents) {
            content ?: continue
            if (content.type == Material.IRON_NUGGET) {
                with(user) {
                    getPlayer().inventory.remove(content)
                }
                this.ammo += amount
            }
        }
    }

    override fun function() {
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
        val p1 = entity.location.toVector()
        p1.y += 0.5
        val loc = this.location.clone().add(0.0, 2.3, 0.0)
        val p2 = this.location.toVector()
        val dst = this.location.distance(entity.location)
        if (dst <= 4) p1.y -= 1
        if (dst <= 1) p1.y -= 2
        if (dst >= 7) p1.y += 1
        val vec = p1.clone().subtract(p2).normalize()
        val arrow = this.location.world.spawnArrow(loc, vec, 0.6F, 12F)
        arrow.pickupStatus = Arrow.PickupStatus.DISALLOWED

        Metadata.provideForEntity(arrow).put(MetadataKeys.SENTRY_ZERO, true)
    }

    override fun registerListeners() {
        Events.subscribe(ProjectileHitEvent::class.java)
            .filter { it.entity is Arrow }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.SENTRY_ZERO].isPresent }
            .handler {
                Schedulers.sync().runLater({
                    if ((it.entity as Arrow).isInBlock) it.entity.remove()
                }, 2L, TimeUnit.SECONDS)
            }
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Arrow }
            .filter { it.entity is Player }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.SENTRY_ZERO].isPresent }
            .handler {
                it.damage = 5.0
                Schedulers.sync().runLater({
                    Metadata.provideForEntity(it.entity).remove(MetadataKeys.SENTRY_ZERO)
                }, 2L, TimeUnit.SECONDS)
            }
    }

    override fun serialize(): JsonElement {
        return serialise("sentry-0")
    }


}