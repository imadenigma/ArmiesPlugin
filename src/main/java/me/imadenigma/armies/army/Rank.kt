package me.imadenigma.armies.army

import java.util.*

enum class Rank(val permissions: Array<Permissions>) {

    EMPEROR(Permissions.values()),
    KNIGHT(
        arrayOf(
            Permissions.COALITION_CHAT,
            Permissions.INVADE,
            Permissions.PROMOTE,
            Permissions.ENEMY,
            Permissions.COALITION,
            Permissions.LEAVE
        )
    ),
    SOLDIER(
        arrayOf(
            Permissions.LEAVE
        )
    ),
    PEASANT(
        arrayOf(
            Permissions.LEAVE
        )
    ),
    PRISONER(
        arrayOf(
            Permissions.COALITION_CHAT
        )
    ),
    NOTHING(arrayOf());

    companion object {
        val sorted = LinkedList(values().toMutableSet())
    }
}