package me.imadenigma.armies

import me.imadenigma.armies.army.ArmyManager
import me.imadenigma.armies.user.UserManager
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.plugin.ap.Plugin
import org.bukkit.plugin.java.JavaPlugin

@Plugin(
    name = "Armies",
    version = "1.12",
    authors = ["Johan Liebert"]
)
class Armies : ExtendedJavaPlugin() {

    var armyManager: ArmyManager = ArmyManager()
    var userManager: UserManager = UserManager()

    override fun enable() {
        // Plugin startup logic
        Configuration()
        userManager.loadUsers()
        armyManager.loadArmies()

    }

    override fun disable() {
        // Plugin shutdown logic
        userManager.loadUsers()
        armyManager.loadArmies()
    }
}