package me.imadenigma.armies.guis

import me.imadenigma.armies.Configuration
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.*
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import me.mattstudios.mfgui.gui.guis.GuiItem
import org.bukkit.inventory.ItemFlag

class ShopGui(val user: User) {
    private val gui: Gui
    private val conf: ConfigurationNode
    private val items = mutableMapOf<Int, GuiItem>()

    init {
        conf = Services.load(Configuration::class.java).config.getNode("shop")
        val rows = conf.getNode("gui-rows").getInt(5)
        val title = conf.getNode("gui-title").getString("title").colorize()
        gui = Gui(rows, title)
        addItems()
        conf.getNode("products","turrets").childrenMap.values.forEach(this::addTurretItem)
        for ((slot, item) in items) {
            gui.setItem(slot, item)
        }
        gui.open(this.user.player!!)
    }

    private fun addItems() {
        val products = this.conf.getNode("products")
        val node = products.getNode("claim-card")
        val price = node.getNode("price").getDouble(100.0) * user.army.cardCount
        val slot = node.getNode("slot").getInt(0)
        val item = parseItem(node)
        val lore = node.getNode("lore").getList { it.toString().replace("{0}", price.toString()).colorize() }
        val method = node.getNode("payment-method").getString("army")
        item.itemMeta.lore.clear()
        items[slot] = ItemBuilder.from(item)
            .setLore(lore)
            .asGuiItem {
                it.isCancelled = true
                if (method.equals("user", true)) {
                    if (user.getBalance() < price) {
                        val msg = this.conf.getNode("money-not-enough").getString("n").replace("{0}", item.itemMeta.displayName)
                        user.msg(msg)
                        return@asGuiItem
                    }

                    user.withdraw(price)
                }else {
                    if (user.army.getBalance() < price) {
                        val msg = this.conf.getNode("money-not-enough").getString("n").replace("{0}", item.itemMeta.displayName)
                        user.msg(msg)
                        return@asGuiItem
                    }
                    user.army.withdraw(price)
                }
                getClaimCard(user.army).give(user)
                val msg = products.parent!!.getNode("success").getString("n").replace("{0}", item.itemMeta.displayName)
                user.msg(msg)
                user.player!!.closeInventory()
            }

        // TODO: 28/05/2021 add more items

    }

    private fun addTurretItem(node: ConfigurationNode) {
        val price = node.getNode("price").getDouble(0.0)
        val slot = node.getNode("slot").getInt(0)
        val nbt = node.getNode("nbt").getString(" - ").split("-")
        val item = parseItem(node)
        items[slot] = ItemBuilder.from(item)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .asGuiItem {
                it.isCancelled = true
                if (user.getBalance() < price) {
                    val msg = this.conf.getNode("money-not-enough").getString("n").replace("{0}", item.itemMeta.displayName)
                    user.msg(msg)
                    return@asGuiItem
                }
                user.withdraw(price)
                if (nbt[0] == "turret") {
                    when {
                        nbt[1] == "gun" -> getGunItem(user.army).give(user)
                        nbt[1] == "manual gun" -> getManualGunItem(user.army).give(user)
                        else -> getSentryItem(user.army).give(user)
                    }
                }else if (nbt[0] == "upgrade") {
                    when {
                        nbt[1] == "gun" -> getGunUpgradeItem(user.army).give(user)
                        nbt[1] == "sentry" -> getSentryUpgradeItem(user.army).give(user)
                        else -> getManualUpgradeItem(user.army).give(user)
                    }
                }
                val msg = this.conf.getNode("success").getString("n").replace("{0}", item.itemMeta.displayName)
                user.msg(msg)
                user.player!!.closeInventory()
            }
    }
}