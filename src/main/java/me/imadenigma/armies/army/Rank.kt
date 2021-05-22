package me.imadenigma.armies.army

enum class Rank(vararg permissions: Permissions) {
    EMPEROR(*Permissions.values()),
    KNIGHT(
        Permissions.COALITION_CHAT,
        Permissions.INVADE,
        Permissions.PROMOTE,
        Permissions.ENEMY,
        Permissions.COALITION
    ),
    SOLDIER(
        Permissions.COALITION_CHAT
    ),
    PEASANT(
        Permissions.COALITION_CHAT
    ),
    PRISONER(
        Permissions.COALITION_CHAT
    ),
    NOTHING
}