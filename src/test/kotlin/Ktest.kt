import junit.framework.TestCase
import me.imadenigma.armies.army.Army
import me.imadenigma.armies.army.Rank
import org.bukkit.Material
import org.bukkit.entity.Player

class Ktest : TestCase() {

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
    fun calcul(vararg note: Double): Double {
        var total = 0.0
        for (i in note) {
            total.plus(i)
        }
        return total / note.size
    }

}