package me.imadenigma.armies.guis

import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.colorize
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import org.bukkit.Material

class RankGui(val user: User) {
    private val gui: Gui
    private val conf: ConfigurationNode

    init {
        conf = Services.load(Configuration::class.java).ranksConf.getNode("ranks")
        val rows = conf.getNode("gui-rows").getInt(5)
        val title = conf.getNode("gui-title").getString("title")
        gui = Gui(rows, title)
        for (rank in Rank.values()) {
            if (rank == Rank.NOTHING) continue
            addItem(rank)
        }
        gui.open(this.user.getPlayer())
    }

    private fun addItem(rank: Rank) {
        val config = conf.getNode(rank.name)
        val glow = config.getNode("glow").boolean
        val material = config.getNode("item").getValue(TypeToken.of(Material::class.java)) ?: Material.MELON
        val slot = config.getNode("slot").getInt(1)
        val name = config.getNode("name").getString(rank.name).colorize()
        this.gui.setItem(
            slot,
            ItemBuilder.from(material).glow(glow).setName(name).asGuiItem {
                it.isCancelled = true
                PermsGui(this.user, rank)
            }
        )
    }
}