package me.imadenigma.armies.utils

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI
import me.lucko.helper.Helper
import org.bukkit.Material
import org.bukkit.block.Block

object HologramBuilder {
    fun updateBlockName(block: Block, name: String) : Boolean {
        var mst = 2.0
        if (block.type == Material.SKULL)
            mst = 1.0
        val opt = HologramsAPI.getHolograms(Helper.hostPlugin()).stream().filter { it.location.equals(block.location.add(0.5,mst,0.5)) }.findAny()
        if (opt.isPresent) {
            opt.get().clearLines()
            opt.get().appendTextLine(name.colorize())
        }
        HologramsAPI.createHologram(Helper.hostPlugin(),block.location.add(0.5, mst, 0.5)).appendTextLine(name.colorize())
        return true
    }
    fun removeBlockName(block: Block) {
        var mst = 2.0
        if (block.type == Material.SKULL)
            mst = 1.0
        val opt = HologramsAPI.getHolograms(Helper.hostPlugin()).stream().filter { it.location.equals(block.location.add(0.5,mst,0.5)) }.findAny()
        if (opt.isPresent) {
            opt.get().clearLines()
        }
    }
}