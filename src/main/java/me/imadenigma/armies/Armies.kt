package me.imadenigma.armies

import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.commands.CommandManager
import me.imadenigma.armies.listeners.PlayerListeners
import me.imadenigma.armies.user.UserManager
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.plugin.ap.Plugin
import me.lucko.helper.utils.Log
import net.milkbowl.vault.economy.Economy


class Armies : ExtendedJavaPlugin() {

    private var armyManager: ArmyManager = ArmyManager()
    private var userManager: UserManager = UserManager()
    var econ: Economy? = null
    override fun enable() {
        // Plugin startup logic
        Configuration()
        userManager.loadUsers()
        armyManager.loadArmies()
        CommandManager()
        if (!setupEcon()) {
            Log.severe("Can't find Vault, please enable vault")
            Helper.plugins().disablePlugin(this)
        }
        registerListeners()
        Services.provide(Armies::class.java, this)
    }

    override fun disable() {
        // Plugin shutdown logic
        userManager.saveUsers()
        armyManager.saveArmies()
    }

    private fun setupEcon(): Boolean {
        if (!Helper.plugins().isPluginEnabled("Vault")) return false
        val rsp = Helper.services().getRegistration(Economy::class.java)
        this.econ = rsp.provider ?: return false
        return true
    }

    private fun registerListeners() {
        PlayerListeners()
    }
}