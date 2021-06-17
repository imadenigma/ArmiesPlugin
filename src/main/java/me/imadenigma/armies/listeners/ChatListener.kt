package me.imadenigma.armies.listeners

import me.imadenigma.armies.user.User
import me.lucko.helper.Helper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import kotlin.streams.toList

class ChatListener : Listener {

    init {
        Helper.hostPlugin().registerListener(this)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChatEvent(e: AsyncPlayerChatEvent) {
        val user = User.getByUUID(e.player.uniqueId)

        if (!user.armyChat || !user.isOnArmy()) {
            val realRecepients = e.recipients.stream().filter { !User.getByUUID(it.uniqueId).armyChat }.toList()
            var R = runCatching {
                e.recipients.clear()
                e.recipients.addAll(realRecepients)
            }
            while (R.isFailure) {
                R = runCatching {
                    e.recipients.clear()
                    e.recipients.addAll(realRecepients)
                }
            }
            return
        }
        if (user.getArmy().chatType == 'c' && user.getArmy().allies.isNotEmpty()) {
            e.isCancelled = true
            val msg = String.format(e.format, e.player.displayName, e.message)
            user.getArmy().allies.forEach {
                it.privateMsg(msg)
            }
            user.getArmy().privateMsg(msg)
            return
        }
        e.isCancelled = true
        val msg = String.format(e.format, e.player.displayName, e.message)
        user.getArmy().privateMsg(msg)
    }
}
