package me.imadenigma.armies.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.utils.compare
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.serialize.Position
import me.lucko.helper.serialize.Region
import me.lucko.helper.text3.Text
import me.lucko.helper.text3.TextComponent
import me.lucko.helper.text3.event.ClickEvent
import me.lucko.helper.text3.event.HoverEvent
import me.lucko.helper.text3.format.TextColor
import me.lucko.helper.text3.format.TextDecoration
import org.bukkit.block.BlockFace


@CommandAlias("a|army")
@Subcommand("map")
class MapCommand : BaseCommand() {

    @Default
    fun default(user: User) {
        val userArmy = if (user.isOnArmy()) user.getArmy()
                else null
        val player = user.getPlayer()!!
        val face = Turrets.yawToFace(player.location.yaw, false)
        player.sendMessage("&8&l&m=================== <&r&3Map&8&l&m> &l&m====================".colorize())
        player.sendMessage("&7&lFacing: &r&3${face.name.toLowerCase()}".colorize())
        val lands = mutableSetOf<Region>()
        if (face == BlockFace.NORTH) {
            val minX = (player.location.x - 25 * 32).toInt()
            val maxX = (player.location.x + 25 * 32).toInt()
            val minZ = (player.location.z - 5 * 32).toInt()
            val maxZ = (player.location.z + 5 * 32).toInt()
            for (z in minZ..maxZ step 32) {
                val line = TextComponent.builder()
                for (x in minX..maxX step 32) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append(
                            TextComponent.builder("+")
                                .color(TextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(TextComponent.of("You are here").color(TextColor.AQUA)))
                                .build()
                        )
                        continue
                    }
                    if (army == null || lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) }) line.append(
                        TextComponent.builder()
                            .content("-")
                            .color(TextColor.DARK_GRAY)
                            .hoverEvent(
                                HoverEvent.showText(
                                    TextComponent.of("Wilderness").decoration(TextDecoration.BOLD, false)
                                        .color(TextColor.GREEN)
                                )
                            )
                            .build()
                    )
                    else {
                        val bool = army.lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) && it.inRegion(
                            Position.of(army.core))
                        }
                        lands.add(army.lands.first { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) })
                        if (army != userArmy || bool) {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of(army.name).decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .build()
                            )
                        }else {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of("Your army's claim, Click to unclaim it")
                                                .decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .clickEvent(ClickEvent.runCommand("/a map unclaim $x;$z"))
                                    .build()
                            )
                        }
                    }
                }
                Text.sendMessage(user.getPlayer()!!, line.build())
            }
        } else if (face == BlockFace.SOUTH) {
            val minX = (player.location.x - 25 * 32).toInt()
            val maxX = (player.location.x + 25 * 32).toInt()
            val minZ = (player.location.z - 5 * 32).toInt()
            val maxZ = (player.location.z + 5 * 32).toInt()
            for (z in maxZ downTo minZ step 32) {
                val line = TextComponent.builder()
                for (x in maxX downTo minX step 32) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append(
                            TextComponent.builder("+")
                                .color(TextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(TextComponent.of("You are here").color(TextColor.AQUA)))
                                .build()
                        )
                        continue
                    }
                    if (army == null || lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) }) line.append(
                        TextComponent.builder()
                            .content("-")
                            .color(TextColor.DARK_GRAY)
                            .hoverEvent(
                                HoverEvent.showText(
                                    TextComponent.of("Wilderness").decoration(TextDecoration.BOLD, false)
                                        .color(TextColor.GREEN)
                                )
                            )
                            .build()
                    )
                    else {
                        val bool = army.lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) && it.inRegion(
                            Position.of(army.core))
                        }
                        lands.add(army.lands.first { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) })

                        if (army != userArmy || bool) {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of(army.name).decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .build()
                            )
                        }else {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of("Your army's claim, Click to unclaim it")
                                                .decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .clickEvent(ClickEvent.runCommand("/a map unclaim $x;$z"))
                                    .build()
                            )
                        }
                    }
                }
                Text.sendMessage(user.getPlayer()!!, line.build())
            }
        } else if (face == BlockFace.EAST) {
            val minX = (player.location.x - 5 * 32).toInt()
            val maxX = (player.location.x + 5 * 32).toInt()
            val minZ = (player.location.z - 25 * 32).toInt()
            val maxZ = (player.location.z + 25 * 32).toInt()
            for (x in maxX downTo minX step 32) {
                val line = TextComponent.builder()
                for (z in minZ..maxZ step 32) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append(
                            TextComponent.builder("+")
                                .color(TextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(TextComponent.of("You are here").color(TextColor.AQUA)))
                                .build()
                        )
                        continue
                    }
                    if (army == null || lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) }) line.append(
                        TextComponent.builder()
                            .content("-")
                            .color(TextColor.DARK_GRAY)
                            .hoverEvent(
                                HoverEvent.showText(
                                    TextComponent.of("Wilderness").decoration(TextDecoration.BOLD, false)
                                        .color(TextColor.GREEN)
                                )
                            )
                            .build()
                    )
                    else {
                        val bool = army.lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) && it.inRegion(
                            Position.of(army.core))
                        }
                        lands.add(army.lands.first { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) })

                        if (army != userArmy || bool) {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of(army.name).decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .build()
                            )
                        }else {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of("Your army's claim, Click to unclaim it")
                                                .decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .clickEvent(ClickEvent.runCommand("/a map unclaim $x;$z"))
                                    .build()
                            )
                        }
                    }
                }
                Text.sendMessage(user.getPlayer()!!, line.build())
            }
        } else if (face == BlockFace.WEST) {
            val minX = (player.location.x - 5 * 32).toInt()
            val maxX = (player.location.x + 5 * 32).toInt()
            val minZ = (player.location.z - 25 * 32).toInt()
            val maxZ = (player.location.z + 25 * 32).toInt()
            for (x in minX..maxX step 32) {
                val line = TextComponent.builder()
                for (z in maxZ downTo minZ step 32) {
                    val army = Army.getByLocation(x.toDouble(), z.toDouble())
                    if (player.location.x compare x && player.location.z compare z) {
                        line.append(
                            TextComponent.builder("+")
                                .color(TextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(TextComponent.of("You are here").color(TextColor.AQUA)))
                                .build()
                        )
                        continue
                    }
                    if (army == null || lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) }) line.append(
                        TextComponent.builder()
                            .content("-")
                            .color(TextColor.DARK_GRAY)
                            .hoverEvent(
                                HoverEvent.showText(
                                    TextComponent.of("Wilderness").decoration(TextDecoration.BOLD, false)
                                        .color(TextColor.GREEN)
                                )
                            )
                            .build()
                    )
                    else {
                        val bool = army.lands.any { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) && it.inRegion(
                            Position.of(army.core))
                        }
                        lands.add(army.lands.first { it.inRegion(Position.of(x.toDouble(), 0.0, z.toDouble(), it.max.world)) })
                        if (army != userArmy || bool) {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of(army.name).decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .build()
                            )
                        }else {
                            line.append(
                                TextComponent.builder()
                                    .content("/")
                                    .color(TextColor.AQUA)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            TextComponent.of("Your army's claim, Click to unclaim it")
                                                .decoration(TextDecoration.BOLD, false)
                                                .color(TextColor.GREEN)
                                        )
                                    )
                                    .clickEvent(ClickEvent.runCommand("/a map unclaim $x;$z"))
                                    .build()
                            )
                        }
                    }
                }
                Text.sendMessage(user.getPlayer()!!, line.build())
            }
        }
    }

    @Subcommand("unclaim")
    @Syntax("<loc>")
    @Private
    fun unclaim(user: User, loc: String) {
        val x = loc.split(';')[0].toDouble()
        val z = loc.split(';')[1].toDouble()
        val army = Army.getByLocation(x, z) ?: return
        val land = army.lands.firstOrNull { it.inRegion(Position.of(x, 0.0, z, it.max.world)) } ?: return
        army.lands.remove(land)
        user.msg("&aarea unclaimed successfully!")
    }

}