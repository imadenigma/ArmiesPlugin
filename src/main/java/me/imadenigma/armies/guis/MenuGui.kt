package me.imadenigma.armies.guis

import me.imadenigma.armies.Configuration
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import me.mattstudios.mfgui.gui.guis.GuiItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.SkullType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

@Suppress("DEPRECATION")
class MenuGui(val user: User) {
    private val gui: Gui
    private val conf: ConfigurationNode

    init {
        conf = Services.load(Configuration::class.java).config.getNode("menu-gui")
        val rows = conf.getNode("rows").getInt(4)
        val title = conf.getNode("title").getString("").colorize()
        gui = Gui(rows, title)
        addContent()
    }

    private fun addContent() {
        val memberGui = Gui(5, "&3Members".colorize())
        for (player in user.getArmy().members) {
            val joueur = Bukkit.getOfflinePlayer(player.uuid)
            val skull = ItemStack(Material.SKULL_ITEM, 1, SkullType.PLAYER.ordinal.toShort())
            val meta = skull.itemMeta as SkullMeta
            meta.setOwningPlayer(joueur)
            skull.itemMeta = meta
            memberGui.addItem(
                ItemBuilder.from(skull)
                    .setSkullOwner(joueur)
                    .setName("&9${joueur.name}".colorize())
                    .setLore("&3${player.rank.name.toLowerCase()}".colorize())
                    .asGuiItem { it.isCancelled = true }
            )
        }
        val skull = ItemStack(Material.SKULL_ITEM, 1, SkullType.PLAYER.ordinal.toShort())

        user.getPlayer()!!.inventory.addItem(skull)
        this.gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })
        this.gui.setItem(11,
            ItemBuilder.from(skull)
                .setName("&3Members".colorize())
                .asGuiItem {
                    it.isCancelled = true
                    memberGui.open(user.getPlayer()!!)
                }
        )
        this.gui.setItem(12,
            ItemBuilder.from(Material.GOLD_NUGGET)
                .setName("&3Shop".colorize())
                .asGuiItem { it.isCancelled = true; ShopGui(user) }
        )

        this.gui.setItem(13,
            ItemBuilder.from(Material.BEACON)
                .setName("&3Core".colorize())
                .setLore("&9HP: ${user.getArmy().hp}".colorize())
                .asGuiItem {
                    it.isCancelled = true
                    user.getPlayer()!!.teleport(user.getArmy().core.location)
                }
        )
        this.gui.setItem(14,
            ItemBuilder.from(Material.CHEST)
                .setName("&3Treasury".colorize())
                .setLore("&9balance: ${user.getArmy().getBalance()}".colorize())
                .asGuiItem { it.isCancelled = true }
        )
        this.gui.setItem(15,
            ItemBuilder.from(Material.REDSTONE)
                .setName("&3Permissions".colorize())
                .setLore("&9Manage permissions for each rank".colorize())
                .asGuiItem {
                    it.isCancelled = true
                    RankGui(user)
                }
        )

        this.gui.open(user.getPlayer()!!)
    }
}