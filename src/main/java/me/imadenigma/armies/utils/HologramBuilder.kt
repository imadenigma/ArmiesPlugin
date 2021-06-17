package me.imadenigma.armies.utils

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI
import me.lucko.helper.Helper
import org.bukkit.block.Block

object HologramBuilder {
    fun updateBlockName(block: Block, name: String) : Boolean {
        val opt = HologramsAPI.getHolograms(Helper.hostPlugin()).stream().filter { it.location.equals(block.location.add(0.5,1.0,0.5)) }.findAny()
        if (opt.isPresent) {
            opt.get().clearLines()
            opt.get().appendTextLine(name.colorize())
        }
        HologramsAPI.createHologram(Helper.hostPlugin(),block.location.add(0.5, 1.0, 0.5)).appendTextLine(name.colorize())
        return true
    }
    fun removeBlockName(block: Block) {
        HologramsAPI.getHolograms(Helper.hostPlugin()).stream()
            .filter { it.location.subtract(0.5, 1.0, 0.5).equalsO(block.location) }
            .findAny()
            .ifPresent {
                it.delete()
            }

    }
}