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
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class ManualFireTurret(
    override val location: Location,
    override val army: Army,
    override var ammo: Int = 100,
    override var hp: Double = 50.0,
    override var damage: Double = 5.0,
    override var distance: Double = 100.0,
    override var level: Int = 1,
    override val uuid: UUID
) : Turrets("Manual gun turret", location, army, ammo, hp, damage, distance, level, uuid) {

    private var isEnabled: Boolean = true
    private var isWorking: Boolean = true
    private lateinit var skull: Block
    init {
        if (this.spawn()) {
            allTurrets.add(this)
            this.registerListeners()
            Schedulers.async().runRepeating({ _ -> this.isEnabled = true }, 40L, 40L)
        }
    }

    override fun serialize(): JsonElement {
        return serialise("manual-gun")
    }

    override fun spawn(): Boolean {
        val block = this.location.block
        if (army.core == block) {
            this.location.add(1.0, 0.0, 0.0)
            return spawn()
        }
        block.type = Material.BLACK_GLAZED_TERRACOTTA
        block.state.update()
        val block2 = block.location.add(0.0, 1.0, 0.0).block
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
        HologramBuilder.removeBlockName(this.location.add(0.0, 0.7, 0.5).block)
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
        this.location.world.spawn(this.skull.getRelative(yawToFace(player.location.yaw, true)).location.add(0.0, 1.5, 0.0), Fireball::class.java) {
            Metadata.provideForEntity(it).put(MetadataKeys.GUN, true)
            it.direction = player.eyeLocation.direction
            it.yield = when (level) {
                1 -> 6F
                2 -> 8F
                else -> 10F
            }
        }
    }

    override fun function() {}

    override fun registerListeners() {
        Events.subscribe(PlayerInteractEvent::class.java)
            .filter { it.action == Action.RIGHT_CLICK_BLOCK }
            .filter { it.clickedBlock == this.skull }
            .handler { e ->
                println("playerFunc")
                this.playerFunc(e.player)
                runCatching {
                    val head = skull.state as Skull
                    head.rotation = yawToFace(e.player.location.yaw, true)
                    head.update()
                }
        }
    }
}