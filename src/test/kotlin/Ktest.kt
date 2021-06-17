
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import org.bukkit.entity.Player
import org.junit.Test

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

    @Test
    fun `assert null safety`() {
        val army: Army? = null
        println(army?.uuid.toString())
    }

    @Test
    fun `assert backsteps in for loop`() {
        for (i in 100 downTo 0 step 10) println(i)
    }


}