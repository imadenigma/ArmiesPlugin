package me.imadenigma.armies.user

import me.imadenigma.armies.Configuration
import me.imadenigma.armies.army.Army
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import java.util.concurrent.TimeUnit

data class Invite(val sender: User, val receiver: User, val army: Army) {
    init {
        allInvites.add(this)
        val durability = Services.load(Configuration::class.java).config.getNode("invite", "durability").getInt(10)
        Schedulers.async().runLater({
            allInvites.remove(this)
        }, durability.toLong(), TimeUnit.SECONDS)
    }

    companion object {
        val allInvites = mutableSetOf<Invite>()
    }
}