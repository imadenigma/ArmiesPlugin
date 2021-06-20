
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import org.bukkit.entity.Player
import org.junit.Test
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

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

    @Test
    fun `assert get yaw is working`() {
        val target = object {
            val x = 10.4
            val z = 10.5
        }
        val origin = object {
            val x = 400.5
            val z = 194.4
        }
        val xDiff = target.x - origin.x
        val zDiff = target.z - origin.z
        val distance = sqrt(xDiff * xDiff + zDiff * zDiff)
        println(acos(xDiff / distance) * 180 / PI)
    }


}