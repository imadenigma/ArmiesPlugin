package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.utils.compare
import me.imadenigma.armies.weapons.Turrets
import org.bukkit.block.BlockFace


@CommandAlias("a|army")
@Subcommand("map")
class MapCommand : BaseCommand() {

    @Default
    fun default(user: User) {
        val player = user.getPlayer()!!
        val strBuilder = mutableListOf<String>()
        val signs = arrayOf("x", "/", "0", "\\", "*", "=", "O", "Z", "1", "2", "3", "4", "5", "6", "7", "8" ,"9")
        val face = Turrets.yawToFace(player.location.yaw, false)
        val map = mutableMapOf<Army, Char>()
        player.sendMessage("&8&l&m=================== <&r&3Map&8&l&m> &l&m====================".colorize())
        player.sendMessage("&7&lFacing: &r&3${face.name.toLowerCase()}".colorize())
        if (face == BlockFace.NORTH) {
            val minX = (player.location.x - 25 * 16).toInt()
            val maxX = (player.location.x + 25 * 16).toInt()
            val minZ = (player.location.z - 5 * 16).toInt()
            val maxZ = (player.location.z + 5 * 16).toInt()
            for (z in minZ..maxZ step 16) {
                val line = StringBuilder()
                for (x in minX..maxX step 16) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append("&a+")
                        continue
                    }
                    if (army == null) line.append("&8-")
                    else {
                        val c = signs[map.size]
                        line.append("&3$c")
                        map.put(army, c.toCharArray()[0])
                    }
                }
                strBuilder.add(line.toString())
            }
        }else if (face == BlockFace.SOUTH) {
            val minX = (player.location.x - 25 * 16).toInt()
            val maxX = (player.location.x + 25 * 16).toInt()
            val minZ = (player.location.z - 5 * 16).toInt()
            val maxZ = (player.location.z + 5 * 16).toInt()
            for (z in maxZ downTo minZ step 16) {
                val line = StringBuilder()
                for (x in minX..maxX step 16) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append("&a+")
                        continue
                    }
                    if (army == null) line.append("&8-")
                    else {
                        val c = signs[map.size]
                        line.append(c)
                        map.put(army, c.toCharArray()[0])
                    }
                }
                strBuilder.add(line.toString())
            }
        }else if (face == BlockFace.EAST) {
            val minX = (player.location.x - 5 * 16).toInt()
            val maxX = (player.location.x + 5 * 16).toInt()
            val minZ = (player.location.z - 25 * 16).toInt()
            val maxZ = (player.location.z + 25 * 16).toInt()
            for (x in maxX downTo minX step 16) {
                val line = StringBuilder()
                for (z in minZ..maxZ step 16) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append("&a+")
                        continue
                    }
                    if (army == null) line.append("&8-")
                    else {
                        val c = signs[map.size]
                        line.append(c)
                        map.put(army, c.toCharArray()[0])
                    }
                }
                strBuilder.add(line.toString())
            }
        }else if (face == BlockFace.WEST) {
            val minX = (player.location.x - 5 * 16).toInt()
            val maxX = (player.location.x + 5 * 16).toInt()
            val minZ = (player.location.z - 25 * 16).toInt()
            val maxZ = (player.location.z + 25 * 16).toInt()
            for (x in minX..maxX step 16) {
                val line = StringBuilder()
                for (z in minZ..maxZ step 16) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append("&a+")
                        continue
                    }
                    if (army == null) line.append("&8-")
                    else {
                        val c = signs[map.size]
                        line.append(c)
                        map.put(army, c.toCharArray()[0])
                    }
                }
                strBuilder.add(line.toString())
            }
        }

        strBuilder.forEach {
            user.msg(it)
        }
        user.msg("&a+&r: &3You are here")
        for ((a,m) in map) {
            user.msg("&a$m&r: &7${a.name}")
        }
    }
}