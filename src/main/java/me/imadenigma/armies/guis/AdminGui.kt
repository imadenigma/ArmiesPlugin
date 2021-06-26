package me.imadenigma.armies.guis

import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.*
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.guis.Gui
import me.mattstudios.mfgui.gui.guis.GuiItem
import org.bukkit.Material

class AdminGui(user: User) {
    val gui: Gui

    init {
        this.gui = Gui(3, "Admin Gui")
        this.gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })
        this.gui.setItem(11, ItemBuilder.from(Material.EMERALD_BLOCK).setName("&3SafeZone Gui".colorize()).asGuiItem {
            it.isCancelled = true
            SafeZoneGui(user)
        })

        this.gui.setItem(15, ItemBuilder.from(Material.FIREBALL).setName("&3US Gui".colorize()).asGuiItem {
            it.isCancelled = true
            UsaGui(user)
        })
        this.gui.open(user.getPlayer()!!)
    }


    inner class SafeZoneGui(user: User) {
        init {
            val gui = Gui(3, "SafeZone Gui")
            gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })
            val army = user.isOnArmy().let { if (it) user.getArmy() else null }
            for (i in 11..15) {
                    gui.setItem(i, ItemBuilder.from(getClaimCard(army)).setLore("&6Safezone Claim Card".colorize(), "&6Right click to claim a land for spawn area".colorize()).asGuiItem {
                        it.isCancelled = true
                        getClaimCard(Army.armies.first { army -> army.name.equals("SafeZone", true) }).give(user)
                        gui.close(user.getPlayer()!!)
                    })
            }
            gui.open(user.getPlayer()!!)
        }
    }

    inner class UsaGui(user: User) {

        init {
            val gui = Gui(3, "USA Gui")
            gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })

            gui.setItem(3, GuiItem(
                    ItemBuilder.from(getGunItem(ArmyManager.consoleArmies.first { it.name.equals("US Army", true) }))
                            .setAmount(1)
                            .setName("&3Gun Turret".colorize())
                            .setUnbreakable(true)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .build()

            ) {
                it.isCancelled = true
                it.currentItem.give(user)
                gui.close(user.getPlayer()!!)
            })

            gui.setItem(4,
                    ItemBuilder.from(getSentryItem(ArmyManager.consoleArmies.first { it.name.equals("US Army", true) }))
                            .setName("&3Sentry turret".colorize())
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.getPlayer()!!)
                            }
            )

            gui.setItem(5,
                    ItemBuilder.from(getManualGunItem(ArmyManager.consoleArmies.first { it.name.equals("US Army", true) }))
                            .setName("&3Manual turret".colorize())
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.getPlayer()!!)
                            }
                    )

            gui.setItem(12,
                ItemBuilder.from(getGunUpgradeItem(ArmyManager.consoleArmies.first { it.name.equals("US Army", true) }))
                        .setName("Gun Upgrade")
                        .setAmount(1)
                        .setLore("&aFree".colorize(), "&6Upgrade your gun turret to a better level".colorize())
                        .asGuiItem {
                            it.isCancelled = true
                            it.currentItem.give(user)
                            gui.close(user.getPlayer()!!)
                        }

            )
            gui.open(user.getPlayer()!!)
        }
    }


}