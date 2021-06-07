import junit.framework.TestCase
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import me.lucko.helper.config.yaml.YAMLConfigurationLoader
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.annotations.TestOnly
import org.junit.Test
import java.io.File

class Ktest {

    fun test1() {
        var str = "my name is {0}, i am {1} yo"
        val infos = arrayOf("johan liebert", 16)
        for (i in infos.indices) {
           str = str.replace("{$i}",infos[i].toString())
        }
        println(str)
    }

    fun test2() {
        Rank.values().sorted().forEach { println(it) }
        println("================================")
        Rank.values().sortedBy { it.ordinal }.forEach { println("$it ordinal: ${it.ordinal}") }
    }

    fun test3() {
        kotlin.runCatching {
            val n: Army? = null
            print(n!!.allies)
        }
        val n = mutableSetOf(1,2,2,2,2,2)
        println(n)
    }

    fun test4() {
        assert(null !is Player)
    }

    lateinit var variable: Player

    @Test
    fun `assert nature of lateinit var`() {
        assert(!::variable.isInitialized)
    }

}