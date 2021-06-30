package me.imadenigma.armies.user

import co.aikar.commands.CommandReplacements

interface Sender {

    fun msgC(path: String)

    fun msgR(msg: String, vararg replacements: Any)

    fun msgCR(path: String, vararg replacements: Any)

    fun msg(msg: String)

}