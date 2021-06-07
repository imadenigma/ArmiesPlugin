package me.imadenigma.armies.exceptions

import me.imadenigma.armies.utils.colorize
import java.lang.RuntimeException
import java.util.*

class ArmyNotFoundException(uuid: String) : RuntimeException("&carmy with the me.imadenigma.armies.weapons.impl.getUuid : $uuid  was not found".colorize())