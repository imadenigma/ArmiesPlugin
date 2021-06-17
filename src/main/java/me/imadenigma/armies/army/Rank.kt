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
            Permissions.LEAVE,
            Permissions.INVITE,
            Permissions.MENU,
            Permissions.DEPOSIT_BALANCE,
            Permissions.OPEN_OR_CLOSE,
            Permissions.HOME,
            Permissions.CHAT

        )
    ),
    SOLDIER(
        arrayOf(
            Permissions.HOME,
            Permissions.CHAT,
            Permissions.LEAVE,
            Permissions.INVITE,
            Permissions.COALITION_CHAT
            )
    ),
    PEASANT(
        arrayOf(
            Permissions.HOME,
            Permissions.CHAT,
            Permissions.LEAVE,
            Permissions.INVITE,
            Permissions.COALITION_CHAT,
            Permissions.LEAVE
        )
    ),
    PRISONER(
        arrayOf()
    ),
    NOTHING(arrayOf());

    companion object {
        val sorted = LinkedList(values().toMutableSet())
    }
}