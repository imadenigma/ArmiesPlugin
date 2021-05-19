package me.imadenigma.armies

import com.google.gson.JsonElement
import org.bukkit.ChatColor
import java.util.*

fun String.colorize(): String {
    return ChatColor.translateAlternateColorCodes('&',this)!!
}

fun JsonElement.toUUID(): UUID {
    return UUID.fromString(this.asString)
}