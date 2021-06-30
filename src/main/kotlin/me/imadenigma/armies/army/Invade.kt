package me.imadenigma.armies.army


import me.lucko.helper.terminable.composite.CompositeTerminable
import java.util.*

class Invade(val attacker: UUID, val defender: UUID)  {
    val terminableConsumer = CompositeTerminable.create()
}




