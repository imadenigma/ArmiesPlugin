package me.imadenigma.armies.weapons.impl

import com.google.gson.JsonElement
import me.imadenigma.armies.army.Army
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
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import java.util.*
import java.util.concurrent.TimeUnit

class FireballTurret(
    override val location: Location,
    override var army: Army?,
    override var ammo: Int = 100,
    override var level: Int = 0,
    override var hp: Double = 50.0,
    override var damage: Double = 5.0,
    override var distance: Double = 8.0,
    override val uuid: UUID = UUID.randomUUID(),
) : Turrets("Gun Turret", location, army, ammo, hp, damage, distance, level, uuid) {

    private var isEnabled: Boolean = true
    private var isWorking: Boolean = true
    private lateinit var block2: Block
    override lateinit var bossbar: BossBar

    init {
        this.bossbar = Bukkit.createBossBar("&3Turret HP".colorize(), BarColor.RED, BarStyle.SEGMENTED_10)
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.sync().runRepeating({ task ->
                run {
                    if (this.hp <= 0.0 || !this.isWorking) {
                        this.despawn()
                        task.close()
                    }
                }
            }, 2L, 7L)
            Schedulers.async().runRepeating({ _ -> this.isEnabled = true }, 40L, 40L)
        }
    }


    override fun serialize(): JsonElement {
        return serialise("gun-$level")
    }

    override fun spawn(): Boolean {
        val block = this.location.block
        if (block.type != Material.AIR) {
            this.location.add(1.0, 0.0, 0.0)
            return spawn()
        }
        block.type = Material.OBSIDIAN
        block.state.update()
        block2 = block.location.add(0.0, 1.0, 0.0).block
        block2.type = Material.SKULL
        setSkullBlock(block2,
            "http://textures.minecraft.net/texture/d6db137a35679beaf790070d0c9c96c90676260ebc00dd2c700562a099db07c0")
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
        block2.type = Material.AIR
        block2.state.update()
        army!!.turrets.remove(this.uuid)
        allTurrets.remove(this)
        HologramBuilder.removeBlockName(block2)
        this.isWorking = false
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
        if (!isEnabled || !isWorking)
            return
        if (this.ammo <= 0) return
        val entity = this.location.world
            .getNearbyEntities(this.location, this.distance, this.distance, this.distance)
            .filterIsInstance(Player::class.java).firstOrNull {
                val user = User.getByUUID(it.uniqueId)
                if (!user.isOnArmy()) true
                else user.getArmy() != this.army && !this.army!!.allies.contains(user.getArmy())
            } ?: return
        this.ammo--
        val p1 = entity.location.subtract(0.0, 1.0, 0.0).toVector()
        val loc = this.location.clone().add(0.0, 2.0, 0.0)
        val p2 = this.location.toVector()
        val vec = p1.clone().subtract(p2)
        this.isEnabled = false
        this.location.world.spawn(loc, Fireball::class.java) {
            Metadata.provideForEntity(it).put(MetadataKeys.GUN, true)
            it.direction = vec.multiply(3)
            it.yield = when (level) {
                1 -> 6F
                2 -> 8F
                else -> 10F
            }
        }
    }

    override fun takeDamage(user: User?) {
        val maxHP: Int
        val bool = user != null
        if (bool) {
            user!!.lastAgg = System.currentTimeMillis()
            if (!this.bossbar.players.contains(user.getPlayer()!!)) this.bossbar.addPlayer(user.getPlayer()!!)
            maxHP = when (this.level) {
                0 -> 50
                1 -> 100
                2 -> 150
                else -> 200
            }
            this.bossbar.progress = (((this.hp * 100) / maxHP) / 100)
            this.bossbar.isVisible = (true)
            Schedulers.sync().runLater(
                {
                    if ((System.currentTimeMillis() - user.lastAgg) / 1000 >= 4 && this.bossbar.players.contains(user.getPlayer()!!))
                        this.bossbar.removePlayer(user.getPlayer()!!)
                }, 5L, TimeUnit.SECONDS)
        }
        this.hp -= 5
        if (this.hp <= 0) {
            if (bool)
                this.bossbar.removePlayer(user!!.getPlayer()!!)
            this.despawn()
        }
    }

    override fun registerListeners() {
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
                println("damage bb $damage!")
                it.damage = 0.0
                (it.entity as Player).damage(this.damage)
            }
    }
}