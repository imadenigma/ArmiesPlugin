package me.imadenigma.armies

import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.serialize.BlockPosition
import me.mattstudios.mfgui.gui.components.ItemBuilder
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

fun String.colorize(): String {
    return ChatColor.translateAlternateColorCodes('&', this)!!
}

fun JsonElement.toUUID(): UUID {
    return UUID.fromString(this.asString)
}

fun getClaimCard(): ItemStack {
    val material = Services.load(Configuration::class.java)
        .config.getNode("shop")
        .getNode("products")
        .getNode("claim-card")
        .getNode("material")
        .getValue(TypeToken.of(Material::class.java))
        ?: Material.MAP
    return ItemBuilder.from(material)
        .setNbt("isCard","true")
        .setName("&3Claim Card".colorize())
        .build()
}

fun ItemStack.give(user: User) {
    if (user.getPlayer().inventory.firstEmpty() == -1) {
        user.getPlayer().world.dropItem(user.getPlayer().location,this)
        return
    }
    user.getPlayer().inventory.addItem(this)
}