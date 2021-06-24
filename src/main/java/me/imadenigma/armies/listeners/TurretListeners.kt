package me.imadenigma.armies.listeners

import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.MetadataKeys
import me.imadenigma.armies.utils.compare
import me.imadenigma.armies.weapons.Turrets
import me.imadenigma.armies.weapons.impl.FireballTurret
import me.imadenigma.armies.weapons.impl.ManualFireTurret
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.Helper
import me.lucko.helper.metadata.Metadata
import me.mattstudios.mfgui.gui.components.ItemNBT
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class TurretListeners : Listener {

    init {
        Helper.hostPlugin().registerListener(this)
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        val user = User.getByUUID(e.player.uniqueId)
        if (ItemNBT.getNBTTag(e.item, "turret") == "sentry") {
            if (e.hasBlock()) Sentry(
                e.clickedBlock.location.add(0.0, 1.0, 0.0),
                user.getArmy(),
                uuid = UUID.randomUUID()
            )
            else Sentry(e.player.location, user.getArmy(), uuid = UUID.randomUUID())
            e.item.amount -= 1
            return
        }
        if (ItemNBT.getNBTTag(e.item, "turret") == "gun") {
            if (e.hasBlock()) FireballTurret(
                e.clickedBlock.location.add(0.0, 1.0, 0.0),
                user.getArmy(),
                uuid = UUID.randomUUID()
            )
            else FireballTurret(e.player.location, user.getArmy(), uuid = UUID.randomUUID())
            println("cc")
            e.item.amount -= 1
            return
        }
        if (ItemNBT.getNBTTag(e.item, "turret") == "manual gun") {
            if (e.hasBlock()) ManualFireTurret(
                e.clickedBlock.location.add(0.0, 1.0, 0.0),
                uuid = UUID.randomUUID()
            )
            e.item.amount -= 1
            return
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(e: BlockBreakEvent) {
        if (Metadata.provideForBlock(e.block)[MetadataKeys.UNBREAKABLE].isPresent) {
            e.isCancelled = true
            Turrets.allTurrets.stream().filter { it.location.x compare e.block.x && it.location.z compare  e.block.z }
                .findAny()
                .ifPresent {
                    it.takeDamage(User.getByUUID(e.player.uniqueId))
                }
        }
    }


    @EventHandler
    fun onBuilding(e: BlockPlaceEvent) {
        val army = Army.getByLocation(e.player.location.x, e.player.location.z) ?: kotlin.run {
            e.isCancelled = false
            return
        }
        val user = User.getByUUID(e.player.uniqueId)
        when {
            !user.isOnArmy() -> {
                e.isCancelled = true
            }
            user.getArmy() != army -> {
                e.isCancelled = true
            }
            !user.hasPermission(Permissions.BUILD) -> {
                e.isCancelled = true
            }
            else -> e.isCancelled = false
        }
    }

    @EventHandler
    fun onBlockDamage(e: BlockDamageEvent) {
        if (Metadata.provideForBlock(e.block)[MetadataKeys.UNBREAKABLE].isPresent) {
            Turrets.allTurrets.stream().filter { it.location.x compare e.block.x && it.location.z compare  e.block.z }
                .findAny()
                .ifPresent {
                    it.takeDamage(User.getByUUID(e.player.uniqueId))
                }
        }
    }

    @EventHandler
    fun onHitBlock(e: ProjectileHitEvent) {
        if (e.hitBlock == null) return
        if (Metadata.provideForBlock(e.hitBlock)[MetadataKeys.UNBREAKABLE].isPresent) {
            Turrets.allTurrets.stream().filter { it.location.x compare e.hitBlock.x && it.location.z compare  e.hitBlock.z }
                .findAny()
                .ifPresent {
                    if (e.entity.shooter is Player)
                        it.takeDamage(User.getByUUID((e.entity.shooter as Player).uniqueId))
                    else it.takeDamage(null)
                }
        }
    }

}