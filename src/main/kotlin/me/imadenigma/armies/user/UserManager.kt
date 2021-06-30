package me.imadenigma.armies.user

import com.google.gson.JsonArray
import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Helper
import me.lucko.helper.gson.GsonProvider
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
            val jsonArray = JsonArray()
            for (user in User.users) {
                jsonArray.add(user.serialize())
            }
            val writer = FileWriter(file)
            GsonProvider.writeElementPretty(writer, jsonArray)
            writer.close()
        }
        Log.info("&3Saving took &c$ms &3ms".colorize())
    }

}