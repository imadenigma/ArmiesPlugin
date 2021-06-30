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

        this.gui.setItem(13, ItemBuilder.from(Material.DIAMOND_SWORD).setName("&3WarZone Gui".colorize()).asGuiItem {
            it.isCancelled = true
            WarZoneGui(user)
        })

        this.gui.setItem(15, ItemBuilder.from(Material.FIREBALL).setName("&3US Gui".colorize()).asGuiItem {
            it.isCancelled = true
            UsaGui(user)
        })
        this.gui.open(user.player!!)
    }


    inner class SafeZoneGui(user: User) {
        init {
            val gui = Gui(3, "SafeZone Gui")
            gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })
            val army = user.isOnArmy().let { if (it) user.army else null }
            for (i in 11..15) {
                gui.setItem(i, ItemBuilder.from(getClaimCard(army)).setLore("&6Safezone Claim Card".colorize(), "&6Right click to claim a land for spawn area".colorize()).asGuiItem {
                    it.isCancelled = true
                    getClaimCard(Army.armies.first { army -> army.name.equals("SafeZone", true) }).give(user)
                    gui.close(user.player!!)
                })
            }
            gui.open(user.player!!)
        }
    }

    inner class WarZoneGui(user: User) {
        init {
            val gui = Gui(3, "WarZone Gui")
            gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })
            val army = user.isOnArmy().let { if (it) user.army else null }
            for (i in 11..15) {
                gui.setItem(i, ItemBuilder.from(getClaimCard(army)).setLore("&WarZone Claim Card".colorize(), "&6Right click to claim a land for spawn area".colorize()).asGuiItem {
                    it.isCancelled = true
                    getClaimCard(Army.armies.first { army -> army.name.equals("War Zone", true) }).give(user)
                    gui.close(user.player!!)
                })
            }
            gui.open(user.player!!)
        }
    }

    inner class UsaGui(user: User) {

        init {
            val gui = Gui(3, "USA Gui")
            gui.filler.fill(GuiItem(Material.STAINED_GLASS_PANE) { it.isCancelled = true })

            gui.setItem(10, GuiItem(
                    ItemBuilder.from(getGunItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setAmount(1)
                            .setName("&3Gun Turret".colorize())
                            .setUnbreakable(true)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .build()

            ) {
                it.isCancelled = true
                it.currentItem.give(user)
                gui.close(user.player!!)
            })

            gui.setItem(11,
                    ItemBuilder.from(getSentryItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setName("&3Sentry turret".colorize())
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.player!!)
                            }
            )

            gui.setItem(12,
                    ItemBuilder.from(getManualGunItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setName("&3Manual turret".colorize())
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Can be used only in this army".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.player!!)
                            }
            )

            gui.setItem(14,
                    ItemBuilder.from(getGunUpgradeItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setName("Gun Upgrade")
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Upgrade your gun turret to a better level".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.player!!)
                            }
            )
            gui.setItem(15,
                    ItemBuilder.from(getSentryUpgradeItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setName("Sentry Upgrade")
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Upgrade your sentry turret to a better level".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.player!!)
                            }
            )
            gui.setItem(16,
                    ItemBuilder.from(getManualUpgradeItem(Army.armies.first { it.name.equals("US Army", true) }))
                            .setName("Manual Gun Upgrade")
                            .setAmount(1)
                            .setLore("&aFree".colorize(), "&6Upgrade your manual gun turret to a better level".colorize())
                            .asGuiItem {
                                it.isCancelled = true
                                it.currentItem.give(user)
                                gui.close(user.player!!)
                            }
            )


            gui.open(user.player!!)
        }
    }


}