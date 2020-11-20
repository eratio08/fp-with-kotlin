package fpkotlin.ch6

import fpkotlin.ch3.Cons
import fpkotlin.ch3.List
import fpkotlin.ch3.Nil

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

/**
 * [linear congruential generator](https://en.wikipedia.org/wiki/Linear_congruential_generator)
 * * a=5DEECE66DL (25214903917)
 * * m=2^48
 * * c=0xBL (11)
 * X_n = (X_n * a + c) mod m
 */
data class SimpleRNG(val seed: Long) : RNG {
    override fun nextInt(): Pair<Int, RNG> {
        val newSeed = (seed * 0x5DEECE66DL + 0xBL) and
                0xFFFFFFFFFFFFL
        val nextRng = SimpleRNG(newSeed)
        val n = (newSeed ushr 16).toInt()
        return n to nextRng
    }
}

interface StateActionSequencer {
    fun nextInt(): Pair<Int, StateActionSequencer>
    fun nextDouble(): Pair<Double, StateActionSequencer>
}

// i1 & i2 will always be equal
fun randomPair(rng: RNG): Pair<Int, Int> {
    val (i1, _) = rng.nextInt()
    val (i2, _) = rng.nextInt()
    return Pair(i1, i2)
}

fun randomPair2(rng: RNG): Pair<Pair<Int, Int>, RNG> {
    val (i1, rng2) = rng.nextInt()
    val (i2, rng3) = rng2.nextInt()
    return Pair(Pair(i1, i2), rng3)
}

// 6.1
fun nonNegativeInt(rng: RNG): Pair<Int, RNG> {
    val (n, rng2) = rng.nextInt()
    val m = when {
        n < 0 -> (Int.MIN_VALUE + 1) * -1
        else -> n
    }
    return m to rng2
}

// 6.2
fun double(rng: RNG): Pair<Double, RNG> {
    val (n, rng2) = nonNegativeInt(rng)
    val m = n / Int.MAX_VALUE.toDouble()
    return m to rng2
}

// 6.3
fun intDouble(rng: RNG): Pair<Pair<Int, Double>, RNG> {
    val (n, rng2) = rng.nextInt()
    val (m, rng3) = double(rng2)
    return (n to m) to rng3
}

fun doubleInt(rng: RNG): Pair<Pair<Double, Int>, RNG> {
    val (n, rng2) = double(rng)
    val (m, rng3) = rng2.nextInt()
    return (n to m) to rng3
}

fun double3(rng: RNG): Pair<Triple<Double, Double, Double>, RNG> {
    val (n, rng2) = double(rng)
    val (m, rng3) = double(rng2)
    val (o, rng4) = double(rng3)
    return Triple(n, m, o) to rng4
}

// 6.4
fun ints(count: Int, rng: RNG): Pair<List<Int>, RNG> {
    tailrec fun loop(n: Int, rng: RNG, out: List<Int>): Pair<List<Int>, RNG> =
            if (n == 0) {
                out to rng
            } else {
                val (i, rng2) = rng.nextInt()
                loop(n - 1, rng2, Cons(i, out))
            }

    return loop(count, rng, Nil)
}

typealias  Rand<A> = (RNG) -> Pair<A, RNG>

val intR: Rand<Int> = { rng -> rng.nextInt() }

fun <A> unit(a: A): Rand<A> = { rng -> Pair(a, rng) }

fun <A, B> map(s: Rand<A>, f: (A) -> B): Rand<B> =
        { rng ->
            val (a, rng2) = s(rng)
            f(a) to rng2
        }

fun nonNegativeEven(): Rand<Int> =
        map(::nonNegativeInt) { it - (it % 2) }

// 6.5
val doubleR: Rand<Double> =
        map(intR, { a -> a / Int.MAX_VALUE.toDouble() })

// 6.6
fun <A, B, C> map2(
        ra: Rand<A>,
        rb: Rand<B>,
        f: (A, B) -> C
): Rand<C> = { rng ->
    val (a, rng2) = ra(rng)
    val (b, rng3) = rb(rng2)
    f(a, b) to rng3
}

fun <A, B> both(ra: Rand<A>, rb: Rand<B>): Rand<Pair<A, B>> =
        map2(ra, rb) { a, b -> Pair(a, b) }

val intDoubleR: Rand<Pair<Int, Double>> = both(intR, doubleR)
val doubleIntR: Rand<Pair<Double, Int>> = both(doubleR, intR)

// 6.7
fun <A> sequence(fs: List<Rand<A>>): Rand<List<A>> =
        List.foldRightL(fs,
                unit(List.empty()),
                { f, acc ->
                    map2(f, acc, { h, t -> Cons(h, t) })
                })

fun ints2(count: Int, rng: RNG): Pair<List<Int>, RNG> {
    tailrec fun loop(n: Int, out: List<Rand<Int>>): List<Rand<Int>> =
            when (n) {
                0 -> Nil
                else -> loop(n - 1, Cons(intR, out))
            }
    return sequence(loop(count, List.empty()))(rng)
}

//fun nonNegativeLessThan_A(n: Int): Rand<Int> =
//        map(::nonNegativeInt) { it % n }
//
//fun nonNegativeLessThan_B(n: Int): Rand<Int> =
//        map(::nonNegativeInt) { i ->
//            val mod = i % n
//            if (i + (n - 1) - mod >= 0) mod
//            else nonNegativeLessThan_B(n)(???)
//        }

// 6.8
fun <A, B> flatMap(f: Rand<A>, g: (A) -> Rand<B>): Rand<B> =
        { rng ->
            val (a, rng2) = f(rng)
            g(a)(rng2)
        }

fun nonNegativeIntLessThan(n: Int): Rand<Int> =
        flatMap(::nonNegativeInt) { i ->
            val mod = i % n
            if (i + (n - 1) - mod >= 0) unit(mod)
            else nonNegativeIntLessThan(n)
        }

// 6.9
fun <A, B> mapF(s: Rand<A>, f: (A) -> B): Rand<B> =
        flatMap(s) { a -> { rng -> f(a) to rng } }

fun <A, B, C> map2F(
        ra: Rand<A>,
        rb: Rand<B>,
        f: (A, B) -> C
): Rand<C> =
        flatMap(ra) { a -> mapF(rb) { b -> f(a, b) } }

fun rollDie(): Rand<Int> = nonNegativeIntLessThan(6)

fun rollDie_B(): Rand<Int> =
        map(nonNegativeIntLessThan(6)) { it + 1 }

/*
-----------
State Monad
-----------
*/

// typealias State<S, A> = (S) -> Pair<A, S>
data class State<S, out A>(val run: (S) -> Pair<A, S>) {
    companion object {
        // 6.10
        fun <S, A> unit(a: A): State<S, A> = State { s -> a to s }
    }
}

// 6.10
fun <S, A, B> State<S, A>.flatMap(f: (A) -> State<S, B>): State<S, B> =
        State { s ->
            val (a, s2) = this.run(s)
            f(a).run(s2)
        }

fun <S, A, B> State<S, A>.map(f: (A) -> B): State<S, B> =
        this.flatMap { a -> State { s -> f(a) to s } }

fun <S, A, B, C> State<S, A>.map2(
        rb: State<S, B>,
        f: (A, B) -> C
): State<S, C> =
        this.flatMap { a -> rb.map { b -> f(a, b) } }

//typealias Rand<A> = State<RNG, A>

fun <S> get(): State<S, S> =
        State { it to it }

fun <S> set(s: S): State<S, Unit> =
        State { Unit to s }

fun <S> modify(f: (S) -> S): State<S, Unit> =
        State { s ->
            val s2 = get<S>().map(f)
            val s3: State<S, Unit> = s2.flatMap { si: S -> set(si) }
            s3.run(s)
        }

// 6.11
sealed class Input
object Coin : Input()
object Turn : Input()
data class Machine(
        val locked: Boolean,
        val candies: Int,
        val coins: Int
)

val update: (Input) -> (Machine) -> Machine =
        { i: Input ->
            { s: Machine ->
                when (i) {
                    is Coin ->
                        if (!s.locked || s.candies == 0) s
                        else s.copy(locked = false, coins = s.coins + 1)
                    is Turn ->
                        if (s.locked || s.candies == 0) s
                        else s.copy(locked = true, candies = s.candies - 1)
                }
            }
        }

fun simulateMachine(
        inputs: List<Input>
): State<Machine, Pair<Int, Int>> {
    val updates = List.map(inputs) { update(it) }
    val modifications = List.map(updates, ::modify)
    return List.foldRightL(modifications,
            State<Machine, Pair<Int, Int>> { m -> (m.candies to m.coins) to m },
            { s: State<Machine, Unit>, t: State<Machine, Pair<Int, Int>> ->
                s.flatMap {
                    get<Machine>().map { it }
                }.flatMap { m ->
                    t.map { (m.candies to m.coins) }
                }
            }
    )
}

fun main() {
    val s = simulateMachine(List.of(
            Coin, Turn,
            Coin, Turn,
            Coin, Turn,
            Coin, Turn
    ))
    println(s.run(Machine(true, 5, 10)))
}