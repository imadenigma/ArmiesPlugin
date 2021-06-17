package me.imadenigma.armies

import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.commands.CommandManager
import me.imadenigma.armies.listeners.ChatListener
import me.imadenigma.armies.listeners.PlayerListeners
import me.imadenigma.armies.listeners.TurretListeners
import me.imadenigma.armies.user.UserManager
import me.imadenigma.armies.weapons.WeaponsManager
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.utils.Log
import net.milkbowl.vault.economy.Economy


class Armies : ExtendedJavaPlugin() {

    private var armyManager = ArmyManager()
    private var userManager = UserManager()
    private var weaponsManager = WeaponsManager()
    lateinit var econ: Economy

    override fun enable() {
        // Plugin startup logic
        Configuration()
        userManager.loadUsers()
        armyManager.loadArmies()
        weaponsManager.loadWeapons()
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
        weaponsManager.saveWeapons()
    }

    private fun setupEcon(): Boolean {
        if (!Helper.plugins().isPluginEnabled("Vault")) return false
        val rsp = Helper.services().getRegistration(Economy::class.java)
        this.econ = rsp.provider ?: return false
        return true
    }

    private fun registerListeners() {
        PlayerListeners()
        TurretListeners()
        ChatListener()
    }
}