package me.imadenigma.armies.weapons

import me.imadenigma.armies.colorize
import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import me.lucko.helper.gson.GsonProvider
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.utils.Log
import java.io.FileReader
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class WeaponsManager {

    fun loadWeapons() {
        Log.info("&aLoading weapons from weapons.json...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("weapons.json")
            for (element in GsonProvider.parser().parse(FileReader(file)).asJsonArray) {
                Turrets.deserialize(element)
            }
        }
        Log.info("&3Loading took &c$ms &3ms".colorize())
    }

    fun saveWeapons() {
        Log.info("&aSaving weapons in cache...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("weapons.json")
            val jsonArray = JsonBuilder.array()
            for (turret in Turrets.allTurrets) {
                jsonArray.add(turret)
            }
            val writer = FileWriter(file)
            GsonProvider.writeElementPretty(writer, jsonArray.build())
            writer.close()
        }
        Log.info("&3Saving took &c$ms &3ms".colorize())
    }

}