package me.imadenigma.armies.army

import com.google.common.reflect.TypeToken
import com.google.gson.JsonArray
import me.imadenigma.armies.colorize
import me.lucko.helper.Helper
import me.lucko.helper.gson.GsonProvider
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.serialize.GsonStorageHandler
import me.lucko.helper.utils.Log
import java.io.FileReader
import java.io.FileWriter
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
}
















