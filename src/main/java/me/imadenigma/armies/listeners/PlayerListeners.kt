package me.imadenigma.armies.listeners

import me.imadenigma.armies.Configuration
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
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.mattstudios.mfgui.gui.components.ItemNBT
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*
import java.util.concurrent.TimeUnit

class PlayerListeners : Listener {

    init {
        Helper.hostPlugin().registerListener(this)
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (!(e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK)) return
        val user = User.getByUUID(e.player.uniqueId)
        if (!user.isOnArmy()) return
        if (ItemNBT.getNBTTag(e.player.inventory.itemInMainHand, "isCard").equals("safezone", true)) {
            if (!MainCommands.checkExistence(user, "name")) return
            val army = Army.getByLocation(e.player.location.x, e.player.location.z)
            if (army != null) {
                e.player.sendMessage("this area is taken")
                return
            } else {
                Army.armies.first { it.name.equals("safezone", true) }.claimArea(e.player.location)
                e.player.sendMessage("you claim the area successfully")
                e.player.inventory.itemInMainHand.amount--
            }

        }
        if (ItemNBT.getNBTTag(e.player.inventory.itemInMainHand, "isCard") == user.getArmy().name) {
            if (!MainCommands.checkExistence(user, "name")) return

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


    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(e: BlockBreakEvent) {
        val army = Army.getByLocation(e.player.location.x, e.player.location.z) ?: return
        val user = User.getByUUID(e.player.uniqueId)
        e.isCancelled = true
        if (!user.isOnArmy()) return
        when {
            e.block == army.core && user.getArmy() != army && (user.rank == Rank.EMPEROR || user.rank == Rank.KNIGHT || user.rank == Rank.SOLDIER) -> {
                army.takeDamage(user)
            }
            user.getArmy() == army && user.hasPermission(Permissions.BREAK) && e.block != army.core -> e.isCancelled =
                    false
        }
    }


    @EventHandler
    fun onDamageBlock(e: BlockDamageEvent) {
        val turret =
                Turrets.allTurrets.firstOrNull { it.location.x compare e.block.x && it.location.z compare e.block.z }
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
            user.getArmy() == army && user.hasPermission(Permissions.BREAK) && e.block != army.core -> e.isCancelled =
                    false
        }
    }

    @EventHandler
    fun onTurretFire(e: ProjectileHitEvent) {
        if (e.hitBlock == null) return
        if (e.hitBlock.type != Material.BEACON) return
        val army = Army.armies.firstOrNull { it.core == e.hitBlock } ?: return
        if (e.entity.shooter is Player)
            army.takeDamage(User.getByUUID((e.entity.shooter as Player).uniqueId))
        else army.takeDamage(null)
    }

    @EventHandler
    fun onLoadTurrets(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val itemInHand = e.player.inventory.itemInMainHand
        val loc = e.clickedBlock.location
        val turret = Turrets.allTurrets.stream()
                .filter { it.location.world == loc.world && it.location.x compare loc.x && it.location.z compare loc.z }
                .findAny()
        if (!turret.isPresent) return
        val user = User.getByUUID(e.player.uniqueId)
        val nbt = ItemNBT.getNBTTag(itemInHand, "upgrade").split("-")
        if (nbt.size != 2) return
        if (nbt[0] == "") return
        if (!user.isOnArmy()) return
        if (nbt[1] == user.getArmy().name)
            when {
                itemInHand.type == Material.IRON_NUGGET -> {
                    turret.get().addAmmo(
                            user, itemInHand.amount
                    )
                    e.player.sendMessage("&aammo added".colorize())
                }
                nbt[0] == "sentry" -> {
                    if (turret.get() is Sentry) {
                        turret.get().upgrade(user)
                        itemInHand.amount--
                    }
                }
                nbt[0] == "gun" -> {
                    if (turret.get() is FireballTurret) {
                        turret.get().upgrade(user)
                        itemInHand.amount--
                    }
                }
                nbt[0] == "manual" -> {
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
        user.isPvp = !(army?.name.equals("safezone", true))
        if (army != null && user.isOutsideArea) {
            e.player.sendTitle("&3&l${army.name}".colorize(), army.description, 10, 70, 20)
            user.isOutsideArea = false
        } else if (army == null && !user.isOutsideArea) {
            e.player.sendTitle("&a&lWilderness".colorize(), "&lIt's dangerous out here!".colorize(), 10, 70, 20)
            user.isOutsideArea = true
        }
    }

    @EventHandler
    fun onCoreHold(e: PlayerInteractEvent) {
        // FOCUS ON THIS Lol
        if (!e.hasBlock() || e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.clickedBlock.type == Material.BEACON) e.isCancelled = true
        val user = User.getByUUID(e.player.uniqueId)
        if (user.rank != Rank.EMPEROR || !user.isOnArmy()) return
        val army = Army.armies.firstOrNull { it.core == e.clickedBlock } ?: return
        if (user.getArmy() != army) return
        if (user.getArmy().invades.isNotEmpty()) {
            user.msg("&cYou can't hold the core while being in a war !")
            return
        }
        user.getPlayer()!!.inventory.addItem(
                getCoreItem(user.getArmy())
        )
        e.clickedBlock.type = Material.AIR
        e.clickedBlock.state.update()

        val config = Services.load(Configuration::class.java).config
        val minutes = config.getNode("core-holding", "minutes").getInt(1)
        val money = config.getNode("core-holding", "percentage-money").getInt(1)
        user.getArmy().lastCoreHolding = System.currentTimeMillis()
        Schedulers.sync().runRepeating({ task ->
            if (!user.isOnArmy()) {
                task.close()
                return@runRepeating
            }
            if (user.getArmy().core.type == Material.BEACON) {
                task.close()
                return@runRepeating
            }
            if ((System.currentTimeMillis() - user.getArmy().lastCoreHolding) / 1000 < TimeUnit.MINUTES.toMillis(
                            minutes.toLong()
                    )) {
                task.close()
                return@runRepeating
            }
            user.getArmy().withdraw(user.getArmy().getBalance() * money / 100)
        }, minutes.toLong(), TimeUnit.MINUTES, minutes.toLong(), TimeUnit.MINUTES)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCorePlace(e: BlockPlaceEvent) {
        if (e.block.type != Material.BEACON) return
        val user = User.getByUUID(e.player.uniqueId)
        val army = Army.getByLocation(e.block.location.x, e.block.location.z)
        if (user.rank != Rank.EMPEROR || !user.isOnArmy()) return
        e.isCancelled = true
        val coreItem = getCoreItem(user.getArmy())
        if (ItemNBT.getNBTTag(e.player.inventory.itemInMainHand, "core") != ItemNBT.getNBTTag(coreItem, "core")) {
            e.isCancelled = true
            return
        }
        if (!user.isOutsideArea && user.getArmy() != army) {
            user.msg("&cyou can't place the core here, this area is claimed")
            return
        }
        if (army == null) {
            e.isCancelled = true
            user.msg("&4You should claim the area then try place your army's core")
            return
        }
        if (user.getArmy() == army) {
            e.isCancelled = false
            Arrays.stream(e.player.inventory.contents)
                    .filter(Objects::nonNull)
                    .filter { ItemNBT.getNBTTag(it, "core") != "" }
                    .forEach { e.player.inventory.removeItem(it) }
            user.getArmy().core = e.block
            user.msg("&3You deplaced your army's core successfully !")

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if (e.damager is Player && e.entity is Player) {
            e.isCancelled = !(User.getByUUID(e.damager.uniqueId).isPvp)
        }
    }
}