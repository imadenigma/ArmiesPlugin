package me.imadenigma.armies.guis

import com.google.common.reflect.TypeToken
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.colorize
import me.imadenigma.armies.getClaimCard
import me.imadenigma.armies.give
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import me.mattstudios.mfgui.gui.guis.GuiItem
import org.bukkit.Material

class ShopGui(val user: User) {
    private val gui: Gui
    private val conf: ConfigurationNode
    init {
        conf = Services.load(Configuration::class.java).config.getNode("shop")
        val rows = conf.getNode("gui-rows").getInt(5)
        val title = conf.getNode("gui-title").getString("title").colorize()
        gui = Gui(rows, title)
        for (pair in addItems().entries) {
            gui.setItem(pair.key,pair.value)
        }
        gui.open(this.user.getPlayer())
    }

    private fun addItems() : MutableMap<Int, GuiItem> {
        val products = this.conf.getNode("products")
        val `return` = mutableMapOf<Int,GuiItem>()
        run {
            val node = products.getNode("claim-card")
            val name = node.getNode("name").getString("null").colorize()
            val price = node.getNode("price").getDouble(100.0)
            val slot = node.getNode("slot").getInt(0)
            val glow = node.getNode("glow").boolean
            val material = node.getNode("material").getValue(TypeToken.of(Material::class.java)) ?: Material.MAP
            val lore = node.getNode("lore").getList(TypeToken.of(String::class.java))
                .map { it.colorize() }
            `return`[slot] = ItemBuilder.from(material).glow(glow).setName(name).setLore(lore).setNbt("isCard", "true")
                .asGuiItem {
                    it.isCancelled = true
                    if (user.getBalance() < price) {
                        val msg = products.parent!!.getNode("money-not-enough").getString("n").replace("{0}",name)
                        user.msg(msg)
                        return@asGuiItem
                    }
                    user.withdraw(price)
                    getClaimCard().give(user)
                    val msg = products.parent!!.getNode("success").getString("n").replace("{0}",name)
                    user.msg(msg)
                    user.getPlayer().closeInventory()
                }
        }
        // TODO: 28/05/2021 add more items

        return `return`
    }
}