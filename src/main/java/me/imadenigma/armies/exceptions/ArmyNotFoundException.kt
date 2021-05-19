package me.imadenigma.armies.exceptions

import me.imadenigma.armies.colorize
import java.lang.RuntimeException
import java.util.*

class ArmyNotFoundException(uuid: String) : RuntimeException("&carmy with the uuid : $uuid  was not found".colorize())