package me.imadenigma.armies.army

import me.imadenigma.armies.Configuration
import me.imadenigma.armies.utils.asLocation
import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.gson.GsonProvider
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.utils.Log
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import kotlin.system.measureTimeMillis

class ArmyManager {


    fun loadArmies() {
        Log.info("&aLoading armies from cache...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("armies.json")
            for (element in GsonProvider.parser().parse(FileReader(file)).asJsonArray) {
                Army.deserialize(element)
            }
            for (army in Army.armies) {
                army.allies.addAll(
                    army.alliesUUID.map { Army.getByUUID(it) }
                )
            }
            val config = Services.load(Configuration::class.java).config.getNode("console-armies")
            val safeZone = Army(
                UUID.randomUUID(),
                "safeZone",
                UUID.randomUUID(),
                mutableSetOf(),
                mutableSetOf(),
                true,
                config.getNode("safezone", "core").getString("").asLocation().block,
                config.getNode("safezone", "core").getString("").asLocation(),
                mutableSetOf(),
                0.0,
                250,
                mutableSetOf(),
                description = config.getNode("safezone", "description").getString("")
            )
            val us = Army(
                UUID.randomUUID(),
                "US Army",
                UUID.randomUUID(),
                core = config.getNode("us", "core").getString("").asLocation().block,
                description = config.getNode("us", "description").getString(""),
                isOpened = true,
                chatType = 'a',
                home = config.getNode("us", "core").getString("").asLocation(),
            )
            consoleArmies = setOf(
                us,
                safeZone
            )


        }
        Log.info("&3Loading took &c$ms &3ms".colorize())
    }

    fun saveArmies() {
        Log.info("&aSaving armies from cache...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("armies.json")
            val jsonArray = JsonBuilder.array()
            for (army in Army.armies) {
                jsonArray.add(army)
            }
            val writer = FileWriter(file)
            GsonProvider.writeElementPretty(writer, jsonArray.build())
            writer.close()
        }
        Log.info("&3Saving took &c$ms &3ms".colorize())
    }

    companion object {
        var consoleArmies = setOf<Army>()
            private set
    }
}
















