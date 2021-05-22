import junit.framework.TestCase

class Ktest : TestCase() {

    fun test1() {
        var str = "my name is {0}, i am {1} yo"
        val infos = arrayOf("johan liebert", 16)
        for (i in infos.indices) {
           str = str.replace("{$i}",infos[i].toString())
        }
        println(str)
    }

}