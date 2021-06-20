package me.imadenigma.armies.listeners

import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.commands.MainCommands
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.*
import me.imadenigma.armies.weapons.Turrets
import me.imadenigma.armies.weapons.impl.FireballTurret
import me.imadenigma.armies.weapons.impl.ManualFireTurret
import me.imadenigma.armies.weapons.impl.Sentry
import me.lucko.helper.Helper
import me.mattstudios.mfgui.gui.components.ItemNBT
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

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
        val army = Army.getByLocation(e.player.location.x, e.player.location.z)
        if (army != null) {
            e.player.sendMessage("this area is taken")
            return
        } else {
            user.getArmy().claimArea(e.player.location)
            e.player.sendMessage("you claim the area successfully")
            e.player.inventory.itemInMainHand.amount--
        }


    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(e: BlockBreakEvent) {
        val army = Army.getByLocation(e.player.location.x, e.player.location.z) ?: return
        val user = User.getByUUID(e.player.uniqueId)
        e.isCancelled = true
        if (!user.isOnArmy()) return
        when {
            e.block == army.core && user.getArmy() != army && (user.rank == Rank.EMPEROR || user.rank == Rank.KNIGHT || user.rank == Rank.SOLDIER) -> {
                army.takeDamage(user)
            }
            user.getArmy() == army && user.hasPermission(Permissions.BREAK) && e.block != army.core -> e.isCancelled = false
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBuildBreak(e: BlockPlaceEvent) {
        val army = Army.getByLocation(e.player.location.x, e.player.location.z) ?: return
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
        }
    }

    @EventHandler
    fun onDamageBlock(e: BlockDamageEvent) {
        val turret = Turrets.allTurrets.firstOrNull { it.location.x compare e.block.x && it.location.z compare e.block.z }
        val user = User.getByUUID(e.player.uniqueId)
        if (turret != null) {
            turret.takeDamage(user)
            if (turret.hp <= 0) turret.despawn()
        }
        if (e.block.type != Material.BEACON) return
        val army = Army.getByLocation(e.player.location.x, e.player.location.z) ?: return
        e.isCancelled = true
        if (!user.isOnArmy()) return
        when {
            e.block == army.core && user.getArmy() != army && (user.rank == Rank.EMPEROR || user.rank == Rank.KNIGHT || user.rank == Rank.SOLDIER) -> {
                army.takeDamage(user)
            }
            user.getArmy() == army && user.hasPermission(Permissions.BREAK) && e.block != army.core -> e.isCancelled = false
        }
    }

    @EventHandler
    fun onLoadTurrets(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val itemInHand = e.player.inventory.itemInMainHand
        val loc = e.clickedBlock.location
        val turret = Turrets.allTurrets.stream().filter { it.location.world == loc.world && it.location.x compare loc.x  && it.location.z compare loc.z }.findAny()
        if (!turret.isPresent) return
        val user = User.getByUUID(e.player.uniqueId)
        if (ItemNBT.getNBTTag(itemInHand, "upgrade") == "") return
        when (itemInHand.type) {
            Material.IRON_NUGGET -> {
                turret.get().addAmmo(
                    user, itemInHand.amount
                )
                e.player.sendMessage("&aammo added".colorize())
            }
            getSentryUpgradeItem().type -> {
                if (turret.get() is Sentry) {
                    turret.get().upgrade(user)
                    itemInHand.amount--
                }
            }
            getGunUpgradeItem().type -> {
                if (turret.get() is FireballTurret) {
                    turret.get().upgrade(user)
                    itemInHand.amount--
                }
            }
            getManualUpgradeItem().type -> {
                if (turret.get() is ManualFireTurret) {
                    turret.get().upgrade(user)
                    itemInHand.amount--
                }
            }
            else -> return
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val user = User.getByUUID(e.entity.uniqueId)
        if (!user.isOnArmy()) return
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip ${e.entity.name} Dead in army !")
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val user = User.getByUUID(e.player.uniqueId)
        val army = Army.getByLocation(e.player.location.x, e.player.location.z)
        if (army != null && user.isOutsideArea) {
            e.player.sendTitle("&3&l${army.name}".colorize(), army.description, 10, 70, 20)
            user.isOutsideArea = false
        }else if (army == null && !user.isOutsideArea)  {
            e.player.sendTitle("&a&lWilderness".colorize(), "&lIt's dangerous out here!".colorize(), 10, 70, 20)
            user.isOutsideArea = true
        }
    }

}