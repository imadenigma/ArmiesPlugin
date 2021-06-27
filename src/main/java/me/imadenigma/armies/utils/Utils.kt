package me.imadenigma.armies.utils

import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.metadata.MetadataKey
import me.mattstudios.mfgui.gui.components.ItemBuilder
import me.mattstudios.mfgui.gui.components.ItemNBT
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.*

private lateinit var claimCard: ItemStack
private lateinit var sentryItem: ItemStack
private lateinit var gunItem: ItemStack
private lateinit var sentryUpItem: ItemStack
private lateinit var gunUpItem: ItemStack
private lateinit var manualGun: ItemStack
private lateinit var manualUpgrade: ItemStack

fun String.colorize(): String {
    return ChatColor.translateAlternateColorCodes('&', this)!!
}

fun JsonElement.asUUID(): UUID {
    return UUID.fromString(this.asString)
}

fun String.asLocation(): Location {
    kotlin.runCatching {
        val words = this.split(';')
        val x = words[0].toDouble()
        val y = words[1].toDouble()
        val z = words[2].toDouble()
        val world = Bukkit.getWorld(words[3])
        return Location(world, x, y, z)
    }
    return Location(Bukkit.getWorlds()[0], 100.0, 80.0, 100.0)
}

fun yawBetweenTwoPoints(target: Location, origin: Location) : Double {
    val xDiff = target.x - origin.x
    val zDiff = target.z - origin.z
    val distance = sqrt(xDiff * xDiff + zDiff * zDiff)
    return (acos(xDiff / distance) * 180 / PI) - 90
}

fun getClaimCard(army: Army?): ItemStack {
    if (::claimCard.isInitialized) return ItemNBT.setNBTTag(claimCard, "isCard", army?.name ?: "null")
    val material = Services.load(Configuration::class.java)
        .config.getNode("shop")
        .getNode("products")
        .getNode("claim-card")
        .getNode("material")
        .getValue(TypeToken.of(Material::class.java))
        ?: Material.MAP
    return ItemBuilder.from(material)
        .setNbt("isCard", army?.name ?: "null")
        .setName("&3Claim Card".colorize())
        .build().also { claimCard = it }
}

fun ItemStack.give(user: User) {

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


infix fun Double.compare(number: Number): Boolean {
    return this.roundToInt() == number.toInt() || this.toString().split(".")[0] == number.toString()
        .split(".")[0] || abs(number.toDouble() - this)  < 1
}

fun getCoreItem(army: Army): ItemStack {
    return ItemBuilder.from(Material.BEACON).setNbt("core", army.name)
        .glow(true)
        .setName("&3Your army's core".colorize())
        .build()
}

/*
 *
 * Turrets Stuff
 *
 */

fun getSentryUpgradeItem(army: Army): ItemStack {
    if (::sentryUpItem.isInitialized) return sentryUpItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "sentry-upgrade")
    return ItemNBT.setNBTTag(parseItem(node), "upgrade", "sentry-${army.name}").also { sentryUpItem = it }
}

fun getGunUpgradeItem(army: Army): ItemStack {
    if (::gunUpItem.isInitialized) return gunUpItem
    val node =
        Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "gun-turret-upgrade")
    return ItemNBT.setNBTTag(parseItem(node), "upgrade", "gun-${army.name}").also { gunUpItem = it }
}

fun getSentryItem(army: Army): ItemStack {
    if (::sentryItem.isInitialized) return sentryItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "sentry")
    return ItemNBT.setNBTTag(parseItem(node), "turret", "sentry-${army.name}").also { sentryItem = it }
}

fun getGunItem(army: Army): ItemStack {
    if (::gunItem.isInitialized) return gunItem
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "gun-turret")
    return ItemNBT.setNBTTag(parseItem(node), "turret", "gun-${army.name}").also { gunItem = it }
}

fun getManualGunItem(army: Army): ItemStack {
    if (::manualGun.isInitialized) return manualGun
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "manual-gun")
    return ItemNBT.setNBTTag(parseItem(node), "turret", "manual-${army.name}").also { manualGun = it }
}

fun getManualUpgradeItem(army: Army): ItemStack {
    if (::manualUpgrade.isInitialized) return manualUpgrade
    val node = Services.load(Configuration::class.java).config.getNode("shop", "products", "turrets", "manual-upgrade")
    return ItemNBT.setNBTTag(parseItem(node), "upgrade", "manual-${army.name}").also { manualUpgrade = it }
}

fun Location.equalsO(location: Location): Boolean {
    return this.x == location.x && this.y == location.y && this.z == location.z && this.world == location.world
}

object MetadataKeys {
    val MANUAL_TURRET: MetadataKey<Boolean> = MetadataKey.createBooleanKey("manual")
    val SENTRY: MetadataKey<Boolean> = MetadataKey.createBooleanKey("sentry")
    val GUN: MetadataKey<Boolean> = MetadataKey.createBooleanKey("gun")
    val UNBREAKABLE: MetadataKey<Boolean> = MetadataKey.createBooleanKey("unbreakable")
}