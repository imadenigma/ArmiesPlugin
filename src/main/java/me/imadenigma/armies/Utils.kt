package me.imadenigma.armies

import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import me.imadenigma.armies.user.User
import me.lucko.helper.Services
import me.lucko.helper.metadata.MetadataKey
import me.mattstudios.mfgui.gui.components.ItemBuilder
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.roundToInt

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
        .setNbt("isCard", "true")
        .setName("&3Claim Card".colorize())
        .build()
}

fun ItemStack.give(user: User) {
    if (user.getPlayer().inventory.firstEmpty() == -1) {
        user.getPlayer().world.dropItem(user.getPlayer().location, this)
        return
    }
    user.getPlayer().inventory.addItem(this)
}

fun distanceToVector(point1: Location, point2: Location): Vector {
    return point1.toVector().subtract(point2.toVector())
}

fun getPrivateField(fieldName: String, clazz: Class<out Any>, any: Any): Any? {
    runCatching {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field[any]
    }
    return null
}
infix fun Double.compare(number: Number) : Boolean {
    return this.roundToInt() == number.toInt() || this.toString().split(".")[0] == number.toString().split(".")[0]
}

object MetadataKeys {
    val SENTRY_ZERO: MetadataKey<Boolean> = MetadataKey.createBooleanKey("sentry0")
}