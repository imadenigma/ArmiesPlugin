package me.imadenigma.armies.weapons.impl

import com.google.gson.JsonElement
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.HologramBuilder
import me.imadenigma.armies.utils.MetadataKeys
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.metadata.Metadata
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Arrow
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.TimeUnit

class ManualFireTurret(
    override val location: Location,
    override var ammo: Int = 100,
    override var level: Int = 1,
    override val uuid: UUID
) : Turrets("Manual gun turret", location, null, ammo, 50.0, 0.0, 0.0, level, uuid) {

    private var isEnabled: Boolean = true
    private var isWorking: Boolean = true
    private lateinit var skull: Block

    override lateinit var bossbar: BossBar

    init {
        bossbar = Bukkit.createBossBar("&3Turret HP".colorize(), BarColor.RED, BarStyle.SEGMENTED_10)
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.sync().runRepeating({ task -> run {
                this.isEnabled = true
                if (this.hp <= 0.0 || !this.isWorking) {
                    this.despawn()
                    task.close()
                }
            } }, 40L, 40L)
        }
    }

    override fun serialize(): JsonElement {
        return serialise("manual-gun")
    }

    override fun spawn(): Boolean {
        val block = this.location.block
        if (block.type != Material.AIR) {
            this.location.add(1.0, 0.0, 0.0)
            return spawn()
        }
        block.type = Material.BLACK_GLAZED_TERRACOTTA
        block.state.update()
        val block2 = block.location.add(0.0, 1.0, 0.5).block
        block2.type = Material.SKULL
        kotlin.runCatching  {
            setSkullBlock(
                block2,
                "http://textures.minecraft.net/texture/d6db137a35679beaf790070d0c9c96c90676260ebc00dd2c700562a099db07c0"
            )
        }
        block2.state.update()
        skull = block2
        Metadata.provideForBlock(block).put(MetadataKeys.UNBREAKABLE, true)
        Metadata.provideForBlock(block2).put(MetadataKeys.UNBREAKABLE, true)
        HologramBuilder.updateBlockName(skull, this.name + " $level")
        return true
    }

    override fun despawn() {
        val block = this.location.block
        block.type = Material.AIR
        block.state.update()
        val block2 = block.getRelative(BlockFace.UP)
        block2.type = Material.AIR
        block2.state.update()
        allTurrets.remove(this)
        this.isWorking = false
        HologramBuilder.removeBlockName(skull)
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

    fun playerFunc(player: Player) {
        if (!isEnabled || !isWorking)
            return
        if (this.ammo <= 0) return
        this.ammo--
        this.isEnabled = false
        if (this.level == 1) {
            val loc = this.location.clone().add(0.0, 2.3, 1.0)
            //this.skull
            //                    .getRelative(yawToFace(player.location.yaw, true)).location
            this.location.world.spawnArrow(
                loc,
                player.eyeLocation.direction,
                1.4F,
                12F
            ).also { Metadata.provideForEntity(it).put(MetadataKeys.MANUAL_TURRET, true) }
        } else {
            this.location.world.spawn(this.skull.getRelative(yawToFace(player.location.yaw, true)).location, Fireball::class.java) {
                Metadata.provideForEntity(it).put(MetadataKeys.MANUAL_TURRET, true)
                it.direction = player.eyeLocation.direction.add(Vector(0, 1, 0))
                it.yield = when (level) {
                    1 -> 6F
                    2 -> 8F
                    else -> 10F
                }
            }
        }
    }

    override fun function() {}

    override fun registerListeners() {
        Events.subscribe(PlayerInteractEvent::class.java)
            .filter { it.action == Action.RIGHT_CLICK_BLOCK }
            .filter { it.clickedBlock == this.skull }
            .handler { e ->
                this.playerFunc(e.player)
                runCatching {
                    val head = skull.state as Skull
                    head.rotation = yawToFace(e.player.location.yaw, true)
                    head.update()
                }
            }
        Events.subscribe(EntityExplodeEvent::class.java, EventPriority.HIGHEST)
            .filter { it.entity is Fireball }
            .filter { Metadata.provideForEntity(it.entity).has(MetadataKeys.MANUAL_TURRET) }
            .handler {
                it.blockList().clear()
            }
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Fireball }
            .filter { Metadata.provideForEntity(it.damager).has(MetadataKeys.MANUAL_TURRET) }
            .filter { it.entity is Player }
            .handler {
                println("damage bb $damage!")
                it.damage = 4.0
                (it.entity as Player).damage(this.damage)
            }

        Events.subscribe(ProjectileHitEvent::class.java)
            .filter { it.entity is Arrow }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.MANUAL_TURRET].isPresent }
            .handler {
                Schedulers.sync().runLater({
                    if ((it.entity as Arrow).isInBlock) it.entity.remove()
                }, 2L, TimeUnit.SECONDS)
            }
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Arrow }
            .filter { it.entity is Player }
            .filter { Metadata.provideForEntity(it.entity)[MetadataKeys.MANUAL_TURRET].isPresent }
            .handler {
                it.damage = 2.0
                Schedulers.sync().runLater({
                    Metadata.provideForEntity(it.entity).remove(MetadataKeys.MANUAL_TURRET)
                }, 2L, TimeUnit.SECONDS)
            }
    }

    override fun takeDamage(user: User?) {
        this.hp -= 5
        val maxHP: Int
        val bool = user != null
        if (bool) {
            user!!.lastAgg = System.currentTimeMillis()
            if (!this.bossbar.players.contains(user.getPlayer()!!)) this.bossbar.addPlayer(user.getPlayer()!!)
            maxHP = when (this.level) {
                0 -> 50
                else -> 100
            }
            this.bossbar.progress = (((this.hp * 100) / maxHP) / 100)
            this.bossbar.isVisible = (true)
            Schedulers.sync().runLater(
                {
                    if ((System.currentTimeMillis() - user.lastAgg) / 1000 >= 4 && this.bossbar.players.contains(user.getPlayer()!!))
                        this.bossbar.removePlayer(user.getPlayer()!!)
                }, 5L, TimeUnit.SECONDS)
        }
        if (this.hp <= 0) {
            if (bool)
                this.bossbar.removePlayer(user!!.getPlayer()!!)
            this.despawn()
        }
    }
}