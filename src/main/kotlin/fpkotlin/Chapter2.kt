package fpkotlin

import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

// f(n) = f(n-1) + f(n-2)
fun fib(n: Int): Int {
    tailrec fun go(n: Int, s1: Int, s2: Int): Int =
            if (n == 0) s1 + s2
            else go(n - 1, s1 + s2, s1)
    return go(n, 1, 0)
}

fun fibNonTail(n: Int): Int {
    fun go(n: Int, s1: Int, s2: Int): Int =
            if (n == 0) s1 + s2
            else go(n - 1, s1 + s2, s1)
    return go(n, 1, 0)
}

@ExperimentalTime
fun measurePerf() {
    fun perf() {
        val times = 10000
        val t1 = measureTime {
            generateSequence(0) { it + 1 }.take(times)
                    .forEach { fib(it) }
        }
        val t2 = measureTime {
            generateSequence(0) { it + 1 }.take(times)
                    .forEach { fibNonTail(it) }
        }
        println("Tailrec: $t1 ms")
        println("Non Tailrec: $t2 ms")
    }
    generateSequence(0) { it + 1 }.take(45)
            .forEach { perf() }
}

// 2.1.2
object Example {
    private fun abs(n: Int): Int =
            if (n < 0) -n
            else n

    private fun factorial(i: Int): Int {
        tailrec fun go(n: Int, acc: Int): Int =
                if (n <= 0) acc
                else go(n - 1, n * acc)
        return go(i, 1)
    }

    fun formatAbs(x: Int): String =
            formatResult("absolut", x, ::abs)

    fun formatFactorial(x: Int): String =
            formatResult("factorial", x, ::factorial)

    private fun formatResult(name: String, n: Int, f: (Int) -> Int): String {
        val msg = "The %s of %d is %d"
        return msg.format(name, n, f(n))
    }
}

fun main() {
    println(Example.formatAbs(-42))
    println(Example.formatFactorial(7))
}

// 2.2.1
fun findFirst(ss: Array<String>, key: String): Int {
    tailrec fun loop(n: Int): Int =
            when {
                n >= ss.size -> -1
                ss[n] == key -> n
                else -> loop(n + 1)
            }
    return loop(0)
}

fun <A> findFirst(xs: Array<A>, p: (A) -> Boolean): Int {
    tailrec fun loop(n: Int): Int =
            when {
                n >= xs.size -> -1
                p(xs[n]) -> n
                else -> loop(n + 1)
            }
    return loop(0)
}

object Exercise2 {
    val <T> kotlin.collections.List<T>.tail: kotlin.collections.List<T>
        get() = drop(1)

    val <T> kotlin.collections.List<T>.head: T
        get() = first()

    fun <A> isSorted(aa: kotlin.collections.List<A>, order: (A, A) -> Boolean): Boolean {
        fun loop(aa: List<A>, sorted: Boolean): Boolean {
            val h = aa.head
            val t = aa.tail
            return if (t.isEmpty()) {
                sorted
            } else {
                loop(t, sorted && order(h, t.head))
            }
        }

        return loop(aa, true)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(isSorted(listOf(0, 1, 2, 3)) { a, b -> a < b })
        println(isSorted(listOf(0, 1, 2, 3)) { a, b -> a > b })
    }
}

// 2.3
fun <A, B, C> partial1(a: A, f: (A, B) -> C): (B) -> C =
        { b -> f(a, b) }

fun <A, B, C> curry(f: (A, B) -> C): (A) -> (B) -> C =
        { a -> { b -> f(a, b) } }

fun <A, B, C> uncurry(f: (A) -> (B) -> C): (A, B) -> C =
        { a, b -> f(a)(b) }

fun <A, B, C> compose(f: (B) -> C, g: (A) -> B): (A) -> C =
        { a: A -> f(g(a)) }