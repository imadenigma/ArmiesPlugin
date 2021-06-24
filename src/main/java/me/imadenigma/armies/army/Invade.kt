package me.imadenigma.armies.army

import me.lucko.helper.promise.Promise
import me.lucko.helper.scheduler.Task
import java.util.*

data class Invade(val attacker: UUID, val defender: UUID) {
    val tasks = mutableSetOf<Task>()
    val promises = mutableSetOf<Promise<Void>>()
}


