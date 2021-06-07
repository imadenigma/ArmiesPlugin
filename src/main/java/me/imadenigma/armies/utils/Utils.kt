package me.imadenigma.armies.utils

import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.metadata.MetadataKey
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.components.ItemNBT
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.roundToInt

private lateinit var claimCard: ItemStack
private lateinit var sentryItem: ItemStack
private lateinit var gunItem: ItemStack
private lateinit var sentryUpItem: ItemStack
private lateinit var gunUpItem: ItemStack

fun String.colorize(): String {
    return ChatColor.translateAlternateColorCodes('&', this)!!
}

fun JsonElement.asUUID(): UUID {
    return UUID.fromString(this.asString)
}


fun getClaimCard(): ItemStack {
    if (::claimCard.isInitialized) return claimCard
    val material = Services.load(Configuration::class.java)
        .config.getNode("shop")
        .getNode("products")
        .getNode("claim-card")
        .getNode("material")
        .getValue(TypeToken.of(Material::class.java))
        ?: Material.MAP
    return ItemBuilder.from(material)
        .setNbt("isCard", "true")
        .setName("&3Claim Card".colorize())
        .build().also { claimCard = it }
}

fun ItemStack.give(user: User) {
    println("reach here")
    if (user.getPlayer()!!.inventory.firstEmpty() == -1) {
        user.getPlayer()!!.world.dropItem(user.getPlayer()!!.location, this)
        return
    }
    user.getPlayer()!!.inventory.addItem(this)
}

fun parseItem(node: ConfigurationNode): ItemStack {
    val name = node.getNode("name").getString("null").colorize()
    val material = Material.matchMaterial(node.getNode("material").getString("")) ?: Material.MELON
    val isGlowing = node.getNode("glow").boolean
    val lore = node.getNode("lore").getList(TypeToken.of(String::class.java))
        .map { it.colorize() }
    return ItemBuilder.from(material)
        .glow(isGlowing)
        .setName(name)
        .setLore(lore)
        .build()
}


infix fun Double.compare(number: Number) : Boolean {
    return this.roundToInt() == number.toInt() || this.toString().split(".")[0] == number.toString().split(".")[0]
}

   /*
    *
    * Turrets Stuff
    *
    */

fun getSentryUpgradeItem(): ItemStack {
    if (::sentryUpItem.isInitialized) return sentryUpItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "sentry-upgrade")
    return ItemNBT.setNBTTag(parseItem(node), "upgrade", "sentry").also { sentryUpItem = it }
}

fun getGunUpgradeItem(): ItemStack {
    if (::gunUpItem.isInitialized) return gunUpItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "gun-turret-upgrade")
    return ItemNBT.setNBTTag(parseItem(node), "upgrade", "gun").also { gunUpItem = it }
}

fun getSentryItem(): ItemStack {
    if (::sentryItem.isInitialized) return sentryItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "sentry")
    return ItemNBT.setNBTTag(parseItem(node), "turret", "sentry").also { sentryItem = it }
}

fun getGunItem(): ItemStack {
    if (::gunItem.isInitialized) return gunItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "gun-turret")
    return ItemNBT.setNBTTag(parseItem(node), "turret", "gun").also { gunItem = it }
}











object MetadataKeys {
    val SENTRY: MetadataKey<Boolean> = MetadataKey.createBooleanKey("sentry")
    val GUN: MetadataKey<Boolean> = MetadataKey.createBooleanKey("gun")
    val UNBREAKABLE: MetadataKey<Boolean> = MetadataKey.createBooleanKey("unbreakable")
}