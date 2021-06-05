package me.imadenigma.armies.listeners

import me.imadenigma.armies.army.Army
import me.imadenigma.armies.colorize
import me.imadenigma.armies.commands.MainCommands
import me.imadenigma.armies.compare
import me.imadenigma.armies.getClaimCard
import me.imadenigma.armies.user.User
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Helper
import me.lucko.helper.serialize.BlockPosition
import me.lucko.helper.serialize.ChunkPosition
import me.lucko.helper.serialize.Position
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.roundToInt

class PlayerListeners : Listener {

    init {
        Helper.hostPlugin().registerListener(this)
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (!(e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK)) return
        val user = User.getByUUID(e.player.uniqueId)
        if (!MainCommands.checkExistence(user, "name")) return
        if (!getClaimCard().isSimilar(e.player.inventory.itemInMainHand)) return
        val pos = Position.of(e.player.location)
        val bool = Army.armies.any { it.lands.any { land -> land.contains(pos) } }
        if (bool) {
            e.player.sendMessage("this area is taken")
            return
        } else {
            user.getArmy().lands.add(ChunkPosition.of(e.player.location.chunk))
            e.player.sendMessage("you claim the area successfully")
            val item = e.player.inventory.itemInMainHand
            e.player.inventory.remove(item)
        }


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(e: BlockBreakEvent) {
        if (!(Army.armies.any { it.lands.any { land -> land.contains(BlockPosition.of(e.block)) } })) return
        val user = User.getByUUID(e.player.uniqueId)
        if (!user.isOnArmy()) {
            e.isCancelled = true
            return
        }
        if (user.getArmy().lands.none { it.contains(BlockPosition.of(e.block)) }) {
            e.isCancelled = true
            return
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBuildBreak(e: BlockPlaceEvent) {
        if (Army.armies.none { it.lands.any { land -> land.contains(BlockPosition.of(e.block)) } }) return
        val user = User.getByUUID(e.player.uniqueId)
        if (!user.isOnArmy()) {
            e.isCancelled = true
            return
        }
        if (user.getArmy().lands.none { it.contains(BlockPosition.of(e.block)) }) {
            e.isCancelled = true
            return
        }

    }

    @EventHandler
    fun onDamageBlock(e: BlockDamageEvent) {
        if (e.block.type != Material.BEACON) return
        print("mok")
        val army = Army.armies.firstOrNull { it.core.location.equals(e.block.location) } ?: return
        army.takeDamage(User.getByUUID(e.player.uniqueId))
    }


    @EventHandler
    fun onLoadTurrets(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val itemInHand = e.player.inventory.itemInMainHand
        if (itemInHand.type == Material.IRON_NUGGET) {
            val loc = e.clickedBlock.location
            Turrets.allTurrets.stream().filter { it.location.world == loc.world && it.location.x compare loc.x  && it.location.z compare loc.z }.findAny().ifPresent {
                it.addAmmo(
                    User.getByUUID(e.player.uniqueId), itemInHand.amount
                )
                e.player.sendMessage("ammo added".colorize())
            }
        }
    }
}