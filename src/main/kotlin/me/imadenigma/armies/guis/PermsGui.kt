package me.imadenigma.armies.guis

import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Permissions
import me.imadenigma.armies.army.Rank
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import org.bukkit.Material

class PermsGui(val user: User, val rank: Rank) {
    private val gui: Gui
    private val conf: ConfigurationNode

    init {
        conf = Services.load(Configuration::class.java).ranksConf.getNode("permissions")
        val rows = conf.getNode("gui-rows").getInt(5)
        val title = conf.getNode("gui-title").getString("title")
        gui = Gui(rows, title)
        for (perm in Permissions.values())
            addItem(perm)
        gui.open(this.user.player!!)
    }

    private fun addItem(permission: Permissions) {
        val config = conf.getNode(permission.name)
        val glow = config.getNode("glow").boolean
        val material = config.getNode("item").getValue(TypeToken.of(Material::class.java)) ?: Material.MELON
        val name = config.getNode("name").getString(permission.name).colorize()
        this.gui.addItem(
            ItemBuilder.from(material)
                .glow(glow)
                .setName(name)
                .setLore("&aRight click to add this permission to the rank".colorize(), "&cLeft click to remove this permission from the rank".colorize())
                .asGuiItem {
                it.isCancelled = true
                if (it.isRightClick) {
                    this.user.army.members.filter { it1 -> it1.rank == rank }.forEach { user1 -> user1.additionalPerms.add(permission); user1.deletedPerms.remove(permission) }
                    this.user.player!!.sendMessage("the permission was added to ${rank.name.toLowerCase()} successfully")
                    this.gui.close(this.user.player!!)
                }else if (it.isLeftClick) {
                    this.user.army.members.filter { it1 -> it1.rank == rank }.forEach { user1 -> user1.deletedPerms.add(permission); user1.additionalPerms.remove(permission) }
                    this.user.player!!.sendMessage("the permission was removed from ${rank.name.toLowerCase()} successfully")
                    this.gui.close(this.user.player!!)
                }

            }
        )
    }
}