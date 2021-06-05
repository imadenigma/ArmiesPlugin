package me.imadenigma.armies.events

import me.imadenigma.armies.weapons.Turrets
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BulletHitEvent(val weapon: Turrets, val isBlock: Boolean, val block: Block?, val entity: Entity?) : Event(true) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        private val handlerList = HandlerList()
    }
}