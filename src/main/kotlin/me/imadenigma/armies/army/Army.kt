package me.imadenigma.armies.army

import com.google.common.base.Preconditions
import com.google.gson.JsonElement
import me.imadenigma.armies.ArmyEconomy
import me.imadenigma.armies.Configuration
import me.imadenigma.armies.exceptions.ArmyNotFoundException
import me.imadenigma.armies.user.Sender
import me.imadenigma.armies.user.User
import me.imadenigma.armies.utils.asUUID
import me.imadenigma.armies.utils.colorize
import me.imadenigma.armies.utils.getCoreItem
import me.imadenigma.armies.weapons.Turrets
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.bossbar.BossBar
import me.lucko.helper.bossbar.BossBarColor
import me.lucko.helper.bossbar.BossBarStyle
import me.lucko.helper.bossbar.BukkitBossBarFactory
import me.lucko.helper.gson.GsonSerializable
import me.lucko.helper.gson.JsonBuilder
import me.lucko.helper.promise.Promise
import me.lucko.helper.serialize.BlockPosition
import me.lucko.helper.serialize.Position
import me.lucko.helper.serialize.Region
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.apache.commons.lang.StringUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class Army(
        val uuid: UUID = UUID.randomUUID(),
        var name: String,
        val owner: UUID,
        val members: MutableSet<User> = mutableSetOf(User.getByUUID(owner)),
        val turrets: MutableSet<UUID> = mutableSetOf(),
        var isOpened: Boolean = false,
        var core: Block,
        var home: Location,
        val lands: MutableSet<Region> = mutableSetOf(),
        private var treasury: Double = 0.0,
        var hp: Int = 250,
        val enemies: MutableSet<Army> = mutableSetOf(),
        val allies: MutableSet<Army> = mutableSetOf(),
        val prisoners: MutableSet<User> = mutableSetOf(),
        val invades: MutableSet<Invade> = mutableSetOf(),
        var chatType: Char = 'a',  //either a or c; a = army, c = coalition,
        var description: String = "Default Army description :(",
        var cardCount: Int = 1,
        val dateOfCreation: LocalDate = LocalDate.now()
) : GsonSerializable, ArmyEconomy, Sender {
    var enemUUID = setOf<UUID>()
    var alliesUUID = setOf<UUID>()
    var lastCoreHolding: Long = 0L
    var lastInvading: Long = 0L

    internal val attackersBB: BossBar
    internal val defendersBB: BossBar
    internal var isDisbanded: Boolean = false

    private lateinit var lastDamager: Army
    private val promises = mutableSetOf<Promise<Void>>()

    init {
        this.members.add(User.getByUUID(this.owner))
        armies.add(this)
        val minLoc = this.core.location.subtract(16.0, 1000.0, 16.0)
        val maxLoc = this.core.location.add(16.0, 1000.0, 16.0)
        lands.add(Region.of(Position.of(minLoc), Position.of(maxLoc)))
        this.attackersBB = BukkitBossBarFactory(Helper.server()).newBossBar()
        this.defendersBB = BukkitBossBarFactory(Helper.server()).newBossBar()
        with(this.attackersBB) {
            progress(1.0)
            color(BossBarColor.RED)
            style(BossBarStyle.SEGMENTED_10)
            title("&3The enemy's Core HP".colorize())
            visible(true)
        }

        with(this.defendersBB) {
            progress(1.0)
            color(BossBarColor.RED)
            style(BossBarStyle.SEGMENTED_10)
            title("&3Your Core's HP".colorize())
            visible(true)
        }
    }

    fun claimArea(center: Location) {
        val minLoc = center.clone().subtract(16.0, 1000.0, 16.0)
        val maxLoc = center.clone().add(16.0, 1000.0, 16.0)
        lands.add(Region.of(Position.of(minLoc), Position.of(maxLoc)))
    }

    fun addMember(user: User) {
        if (user.isOnArmy()) {
            user.army.kickMember(user)
        }
        if (ArmyManager.consoleArmies.contains(this)) {
            user.additionalPerms.addAll(
                    arrayOf(
                            Permissions.CHAT,
                            Permissions.COALITION_CHAT,
                            Permissions.HOME,
                            Permissions.LEAVE,
                            Permissions.INVITE,
                    )
            )
            user.deletedPerms.addAll(
                    arrayOf(
                            Permissions.COALITION,
                            Permissions.WITHDRAW_BALANCE,
                            Permissions.BREAK,
                            Permissions.BUILD,
                            Permissions.DEMOTE,
                            Permissions.PERM,
                            Permissions.PROMOTE,
                            Permissions.ClAIM,
                            Permissions.SURRENDER,
                            Permissions.DESCRIPTION,
                            Permissions.KICK,
                            Permissions.SHOP,
                            Permissions.MENU,
                            Permissions.DISBAND
                    )
            )
        }
        user.armyChat = true
        user.rank = Rank.PEASANT
        this.members.add(user)
    }

    fun kickMember(user: User) {
        this.members.remove(user)
        this.prisoners.remove(user)
        if (user.player != null) {
            armies.filter { it.invades.isNotEmpty() }
                    .forEach { it.attackersBB.removePlayer(user.player!!); it.defendersBB.removePlayer(user.player!!) }
        }
        user.additionalPerms.clear()
        user.deletedPerms.clear()
        user.rank = Rank.NOTHING
        user.armyChat = false
        if (ArmyManager.consoleArmies.contains(this)) return
        if (this.members.size == 0) {
            this.members.forEach { it.rank = Rank.NOTHING }
            this.prisoners.forEach { it.rank = Rank.NOTHING }
            this.disband()
        }
    }

    fun kill(army: Army) {
        if (ArmyManager.consoleArmies.contains(this)) return
        if (ArmyManager.consoleArmies.contains(army)) return
        this.defendersBB.visible(false)
        this.attackersBB.visible(false)
        army.core.type = Material.AIR
        army.core.state.update()
        for (invade in invades) {
            if (invade.defender == army.uuid && invade.attacker == this.uuid) {
                this.deposit(this.treasury * 0.7)
                break
            }
            if (invade.attacker == army.uuid && invade.defender == this.uuid) {
                this.deposit(this.treasury * 0.4)
                break
            }
        }
        army.members.forEach {
            this.prisoners.add(it)
            it.rank = Rank.PRISONER
        }
        army.prisoners.forEach {
            this.prisoners.add(it)
            it.rank = Rank.PRISONER
        }
        army.turrets.forEach {
            this.turrets.add(it)
            Turrets.allTurrets.filter { turret -> turret.army == army }
                    .forEach { turret -> turret.army = this }
        }
        this.lands.addAll(army.lands)
        army.disband()
    }

    fun takeDamage(damager: User?) {
        if (ArmyManager.consoleArmies.contains(this)) return
        if (damager != null) {
            if (!damager.isOnArmy()) return
            if (damager.army.getBalance() < 100000) return
            if (this.members.contains(damager) || this.prisoners.contains(damager)) return
            if (this.invades.none { it.attacker == damager.army.uuid || it.defender == damager.army.uuid }) {
                if (damager.rank == Rank.EMPEROR || damager.rank == Rank.SOLDIER || damager.rank == Rank.KNIGHT) {
                    val invade = Invade(damager.army.uuid, this.uuid)
                    val damagerMembers = mutableSetOf<Player>()
                    damagerMembers.addAll(damager.army.members.mapNotNull { it.player })
                    damagerMembers.addAll(damager.army.prisoners.mapNotNull { it.player })
                    damagerMembers.addAll(this.members.mapNotNull { it.player })
                    damagerMembers.addAll(this.prisoners.mapNotNull { it.player })
                    damagerMembers.forEach { if (!this@Army.isDisbanded) it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("30 minutes left")) }
                    val promise = with(Promise.start()) {

                        this.thenRunDelayedSync({ if (!this@Army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("20 minutes left")) } }, 10L, TimeUnit.SECONDS)
                                .thenRunDelayedSync({ if (!this@Army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("10 minutes left")) } }, 10L, TimeUnit.SECONDS)
                                .thenRunDelayedSync({ if (!this@Army.isDisbanded) damagerMembers.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("5 minutes left")) } }, 5L, TimeUnit.SECONDS)
                                .thenRunDelayedSync({
                                    var i = 11
                                    Schedulers.sync().runRepeating({ task ->
                                        i--
                                        if (i >= 0) damagerMembers.forEach { if (!this@Army.isDisbanded) it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("$i seconds left")) }
                                        else if (i <= 0 && !this@Army.isDisbanded) {
                                            this@Army.kill(damager.army)
                                            task.close()
                                            this.close()
                                        } else if (i <= 0) {
                                            task.close()
                                            this.close()
                                            task.stop()
                                            task.close()
                                        }
                                    }, 1L, TimeUnit.SECONDS, 1L, TimeUnit.SECONDS).bindWith(invade.terminableConsumer)


                                }, TimeUnit.SECONDS.toMillis(5) - TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS)

                    }
                    this.promises.add(promise)
                    this.invades.add(invade)
                    damager.army.invades.add(invade)
                    val msg =
                            Services.load(Configuration::class.java).config.getNode("invading-broadcast").getString("")
                    Helper.server()
                            .broadcastMessage(
                                    msg.colorize().replace("{0}", damager.army.name).replace("{1}", this.name)
                            )
                    this.attackersBB.addPlayers(damager.army.members.mapNotNull { it.player })
                    this.defendersBB.addPlayers(this.members.mapNotNull { it.player })
                } else return
            }
        }
        if (damager != null) lastDamager = damager.army
        this.hp -= 5
        val progress = ((this.hp * 100).toDouble() / 250) / 100
        this.attackersBB.progress(progress)
        this.defendersBB.progress(progress)
        if (this.hp <= 0) {
            if (damager != null) damager.player!!.server.broadcastMessage("&aThe army &c${damager.army.name} &awon the war against &c${this.name}".colorize())
        } else return
        invades.first { (it.attacker == this.uuid && it.defender == lastDamager.uuid) || (it.defender == this.uuid && it.attacker == lastDamager.uuid) }
        damager?.army?.kill(this) ?: lastDamager.kill(this)
        this@Army.defendersBB.visible(false)
        this@Army.attackersBB.visible(false)
    }

    fun disband() {
        this.isDisbanded = true
        this.core.type = Material.AIR
        this.core.state.update()
        armies.stream().forEach { it1 ->
            val invade = it1.invades.stream().filter { it.defender == this.uuid || it.attacker == this.uuid }.findAny()
                    .orElse(null)
            it1.invades.remove(invade)
            it1.enemies.remove(this)
            it1.allies.remove(this)
        }
        val user = User.getByUUID(this.owner)
        user.player?.inventory?.remove(getCoreItem(this))
        Turrets.allTurrets.filter { it.army == this }.forEach { it.despawn() }
        this.promises.forEach {
            it.cancel()
            it.close()
        }
        armies.remove(this)
        this.enemies.clear()
        this.invades.clear()
        this.members.clear()
        this.turrets.clear()
        this.name = ""
        this.isOpened = false
        this.prisoners.clear()
        this.lands.clear()
    }

    fun privateMsg(msg: String) {
        for (member in this.members) {
            if (!member.armyChat) return
            member.msg(msg)
        }
    }

    override fun serialize(): JsonElement {
        val jsMembers = JsonBuilder.array()
                .addAll(this.members.stream().map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
        val pos = BlockPosition.of(this.core).serialize()
        val home = Position.of(this.home).serialize()
        val enem = JsonBuilder.array().addAll(this.enemies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val alli = JsonBuilder.array().addAll(this.allies.map { JsonBuilder.primitive(it.uuid.toString()) })
        val invades =
                JsonBuilder.array().addAll(this.invades.map { JsonBuilder.primitive("${it.attacker}??${it.defender}") })
        val areas = JsonBuilder.array().addAll(this.lands.map { it.serialize() })
        val turrets = JsonBuilder.array().addAll(this.turrets.map { JsonBuilder.primitive(it.toString()) })
        return JsonBuilder.`object`()
                .add("uuid", this.uuid.toString())
                .add("name", this.name)
                .add("owner", this.owner.toString())
                .add("members", jsMembers.build())
                .add("turrets", turrets.build())
                .add("isOpened", this.isOpened)
                .add("core", pos)
                .add("home", home)
                .add("areas", areas.build())
                .add("treasury", this.treasury)
                .add("hp", this.hp)
                .add("enemies", enem.build())
                .add("allies", alli.build())
                .add("chattype", this.chatType)
                .add("invades", invades.build())
                .add("description", this.description)
                .add("cardCount", this.cardCount)
                .add("date", dateOfCreation.toString())
                .add(
                        "prisoners",
                        JsonBuilder.array().addAll(this.prisoners.map { JsonBuilder.primitiveNonNull(it.uuid.toString()) })
                                .build()
                )
                .build()
    }

    override fun deposit(amount: Double) {
        Preconditions.checkArgument(amount >= 0, "Amount must be positive")
        this.treasury += amount
    }

    override fun withdraw(amount: Double) {
        Preconditions.checkArgument(amount >= 0, "Amount must be positive")
        if (this.treasury - amount >= 0) {
            this.treasury -= amount
            return
        }
        this.treasury = 0.0
    }

    override fun getBalance(): Double {
        return this.treasury
    }

    override fun msgC(path: String) {
        val strs = StringUtils.split(path, " ")
        var node = User.language!!
        for (str in strs) node = node.getNode(str)
        val msg = node.string ?: return
        for (player in this.members.mapNotNull { it.player }) {
            player.sendMessage(msg.colorize())
        }
    }

    override fun msgR(msg: String, vararg replacements: Any) {
        var s = msg
        for (i in replacements.indices) {
            s = s.replace("{$i}", replacements[i].toString())
        }
        for (player in this.members.mapNotNull { it.player }) {
            player.sendMessage(s.colorize())
        }
    }

    override fun msgCR(path: String, vararg replacements: Any) {
        val strs = StringUtils.split(path, " ")
        var node = User.language!!
        for (str in strs) node = node.getNode(str)
        var msg = node.getString("nullM")
        for (i in replacements.indices) {
            msg = msg.replace("{$i}", replacements[i].toString())
        }
        for (player in this.members.mapNotNull { it.player }) {
            player.sendMessage(msg.colorize())
        }
    }

    override fun msg(msg: String) {
        for (player in this.members.mapNotNull { it.player }) {
            player.sendMessage(msg.colorize())
        }
    }

    fun addPromise(promise: Promise<Void>) {
        this.promises.add(promise)
    }


    companion object {
        val armies = mutableSetOf<Army>()

        fun deserialize(jsonElement: JsonElement): Army {
            val obj = jsonElement.asJsonObject
            val uuid = UUID.fromString(obj.get("uuid").asString)
            val name = obj.get("name").asString
            val owner = UUID.fromString(obj.get("owner").asString)
            val members = obj.get("members").asJsonArray.map { User.getByUUID(it.asUUID()) }.toMutableSet()
            val turrets = obj.get("turrets").asJsonArray.map { it.asUUID() }.toMutableSet()
            val isOpened = obj.get("isOpened").asBoolean
            val core = BlockPosition.deserialize(obj.get("core")).toBlock()
            val home = Position.deserialize(obj.get("home")).toLocation()
            val areas = obj.get("areas").asJsonArray.map { Region.deserialize(it) }.toMutableSet()
            val treasury = obj.get("treasury").asDouble
            val enemies = obj.get("enemies").asJsonArray.map { it.asUUID() }.toMutableSet()
            val allies = obj.get("allies").asJsonArray.map { it.asUUID() }.toMutableSet()
            val prisoners = obj.get("prisoners").asJsonArray.map { User.getByUUID(it.asUUID()) }.toMutableSet()
            val chatType = obj.get("chattype").asCharacter
            val description = obj.get("description").asString
            val cardCount = obj.get("cardCount").asInt
            val creationDate = obj.get("date").asString
            val invades = obj["invades"].asJsonArray.map {
                val str = it.asString
                val words = str.split("??")
                var attacker: UUID? = null
                var defender: UUID? = null
                kotlin.runCatching {
                    attacker = UUID.fromString(words[0])
                    defender = UUID.fromString(words[1])
                }
                Invade(attacker!!, defender!!)
            }.toMutableSet()
            val hp = obj.get("hp").asInt
            val army =
                    Army(
                            uuid,
                            name,
                            owner,
                            members,
                            turrets,
                            isOpened,
                            core,
                            home,
                            areas,
                            0.0,
                            hp,
                            prisoners = prisoners,
                            invades = invades,
                            chatType = chatType,
                            description = description,
                            cardCount = cardCount,
                            dateOfCreation = LocalDate.parse(creationDate)
                    )
            army.deposit(treasury)
            army.enemUUID = enemies
            army.alliesUUID = allies
            return army
        }

        fun getByUUID(uuid: UUID): Army {
            return this.armies.stream().filter { it.uuid == uuid }.findAny()
                    .orElseThrow { ArmyNotFoundException(uuid.toString()) }
        }

        fun getByLocation(x: Double, z: Double): Army? {
            return armies.firstOrNull { it.lands.any { land -> land.inRegion(Position.of(x, 0.0, z, land.min.world)) } }
        }
    }


}


