package me.imadenigma.armies.user

import me.imadenigma.armies.army.Army
import me.imadenigma.armies.utils.*
import me.lucko.helper.Helper
import me.lucko.helper.gson.GsonProvider
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.utils.Log
import java.io.FileReader
import java.io.FileWriter
import kotlin.system.measureTimeMillis

class UserManager {

    fun loadUsers() {
        Log.info("&aLoading users from cache...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("users.json")
            for (element in GsonProvider.parser().parse(FileReader(file)).asJsonArray) {
                User.deserialize(element)
            }
        }
        Log.info("&3Loading took &c$ms &3ms".colorize())
    }

    fun saveUsers() {
        Log.info("&aSaving users from cache...".colorize())
        val ms = measureTimeMillis {
            val file = Helper.hostPlugin().getBundledFile("users.json")
            val jsonArray = JsonBuilder.array()
            for (user in User.users) {
                jsonArray.add(user)
            }
            val writer = FileWriter(file)
            GsonProvider.writeElementPretty(writer, jsonArray.build())
            writer.close()
        }
        Log.info("&3Saving took &c$ms &3ms".colorize())
    }

}