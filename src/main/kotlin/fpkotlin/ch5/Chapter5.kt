package fpkotlin.ch5

import fpkotlin.ch3.List
import fpkotlin.ch3.Nil
import fpkotlin.ch4.None
import fpkotlin.ch4.Option
import fpkotlin.ch4.Some

sealed class Stream<out A> {

    abstract fun isEmpty(): Boolean

    companion object {
        // smart constructors
        fun <A> cons(hd: () -> A, tl: () -> Stream<A>): Stream<A> {
            val head: A by lazy(hd)
            val tail: Stream<A> by lazy(tl)
            return Cons({ head }, { tail })
        }

        fun <A> of(vararg xs: A): Stream<A> =
                if (xs.isEmpty()) empty()
                else cons({ xs[0] }, { of(*xs.sliceArray(1 until xs.size)) })

        fun <A> empty(): Stream<A> = Empty
    }
}

data class Cons<out A>(
        val head: () -> A,
        val tail: () -> Stream<A>
) : Stream<A>() {
    override fun isEmpty(): Boolean = false
}

object Empty : Stream<Nothing>() {
    override fun isEmpty(): Boolean = true
}

fun <A> Stream<A>.headOption(): Option<A> =
        when (this) {
            is Empty -> None
            is Cons -> Some(head())
        }

// 5.1
fun <A> Stream<A>.toList(): List<A> {
    tailrec fun loop(sa: Stream<A>, la: List<A>): List<A> =
            when (sa) {
                is Empty -> la
                is Cons -> loop(sa.tail(), fpkotlin.ch3.Cons(sa.head(), la))
            }

    return List.reverse(loop(this, Nil))
}

// 5.2
fun <A> Stream<A>.take(n: Int): Stream<A> {
    tailrec fun loop(n: Int, sa: Stream<A>, out: Stream<A>): Stream<A> =
            when {
                n == 0 -> out
                sa is Empty -> out
                else -> {
                    sa as Cons
                    loop(n - 1, sa.tail(), Stream.cons(sa.head, { out }))
                }
            }
    return loop(n, this, Empty)
}

fun <A> Stream<A>.drop(n: Int): Stream<A> {
    tailrec fun loop(n: Int, sa: Stream<A>): Stream<A> =
            when {
                n == 0 -> sa
                sa is Empty -> Empty
                else -> {
                    sa as Cons
                    loop(n - 1, sa.tail())
                }
            }
    return loop(n, this)
}

// 5.3
fun <A> Stream<A>.takeWhile(p: (A) -> Boolean): Stream<A> {
    tailrec fun loop(si: Stream<A>, so: Stream<A>): Stream<A> =
            when (si) {
                is Empty -> so
                is Cons ->
                    if (p(si.head())) loop(si.tail(), Stream.cons(si.head, { so }))
                    else loop(Stream.empty(), so)
            }
    return loop(this, Stream.empty())
}

fun <A> Stream<A>.exists(p: (A) -> Boolean): Boolean =
        when (this) {
            is Empty -> false
            is Cons -> p(head()) || tail().exists(p)
        }

fun <A, B> Stream<A>.foldRight(z: () -> B, f: (A, () -> B) -> B): B =
        when (this) {
            is Empty -> z()
            is Cons -> f(head()) { tail().foldRight(z, f) }
        }

fun <A> Stream<A>.existsF(p: (A) -> Boolean): Boolean =
        foldRight({ false }, { a, b -> p(a) || b() })

// 5.4
fun <A> Stream<A>.forAll(p: (A) -> Boolean): Boolean =
        foldRight({ true }, { h, t -> p(h) && t() })

// 5.5 -- some what behaves not as expected -- works more like filter here
fun <A> Stream<A>.takeWhileF(p: (A) -> Boolean): Stream<A> =
        foldRight({ Stream.empty() }) { h, t ->
            // see how the rest is lazy and will only be
            // executed if something like toList is called
            if (p(h)) Stream.cons({ h }, t) else {
                Stream.empty()
            }
        }

// 5.6
fun <A> Stream<A>.headOptionF(): Option<A> =
        foldRight({ Option.empty() }) { a, _ ->
            // trick here is to omit the tail call
            // this skips any further recursion
            // as f(head()) { tail().foldRight(z, f) } includes
            // the recursive call of foldRight
            Some(a)
        }

// 5.7
fun <A, B> Stream<A>.map(f: (A) -> B): Stream<B> =
        foldRight({ Stream.empty() }) { a, next -> Stream.cons({ f(a) }, next) }

fun <A> Stream<A>.filter(p: (A) -> Boolean): Stream<A> =
        foldRight({ Stream.empty() }) { h, t ->
            if (p(h)) Stream.cons({ h }, t) else t()
        }

fun <A> Stream<A>.append(xs: () -> Stream<A>): Stream<A> =
        foldRight(xs) { h, t -> Stream.cons({ h }, t) }

fun <A, B> Stream<A>.foldRight(f: (A) -> Stream<B>): Stream<B> =
        foldRight({ Stream.empty() }) { h, t -> f(h).append(t) }

fun <A> Stream<A>.find(p: (A) -> Boolean): Option<A> =
        filter(p).headOptionF()

fun ones(): Stream<Int> = Stream.cons({ 1 }, { ones() })

// 5.8
fun <A> constant(a: A): Stream<A> = Stream.cons({ a }, { constant(a) })

// 5.9
fun from(n: Int): Stream<Int> = Stream.cons({ n }, { from(n + 1) })

// 5.10
fun fibs(): Stream<Int> {
    fun loop(curr: Int, nxt: Int): Stream<Int> =
            Stream.cons({ curr }, { loop(nxt, curr + nxt) })
    return loop(0, 1)
}

// 5.11 -- co-recursion also named guarded recursion
fun <A, S> unfold(z: S, f: (S) -> Option<Pair<A, S>>): Stream<A> =
        when (val res = f(z)) {
            is None -> Stream.empty()
            is Some -> Stream.cons({ res.get.first }, { unfold(res.get.second, f) })
        }

// 5.12
fun fibsU(): Stream<Int> =
        unfold(Pair(0, 1)) { (curr, next) ->
            Some(Pair(curr, Pair(next, curr + next)))
        }

fun fromU(n: Int): Stream<Int> =
        unfold(n) { Some(Pair(it, it + 1)) }

fun constantU(n: Int): Stream<Int> =
        unfold(n) { Some(it to it) }

fun onesU(): Stream<Int> =
        unfold(1) { Some(1 to 1) }

// 5.13
fun <A, B> Stream<A>.mapU(f: (A) -> B): Stream<B> =
        unfold(this) {
            when (it) {
                is Empty -> None
                is Cons -> Some(Pair(f(it.head()), it.tail()))
            }
        }

fun <A> Stream<A>.takeU(n: Int): Stream<A> =
        unfold(Pair(n, this)) { (n, l) ->
            when {
                n == 0 -> None
                l is Empty -> None
                l is Cons -> Some(Pair(l.head(), Pair(n - 1, l.tail())))
                else -> None
            }
        }

fun <A> Stream<A>.takeWhileU(p: (A) -> Boolean): Stream<A> =
        unfold(this) {
            when (it) {
                is Empty -> None
                is Cons ->
                    if (p(it.head())) Some(Pair(it.head(), it.tail()))
                    else None
            }
        }

fun <A, B, C> Stream<A>.zipWith(
        that: Stream<B>,
        f: (A, B) -> C
): Stream<C> =
        unfold(Pair(this, that)) { (ti, ta) ->
            when (ti) {
                is Empty -> None
                is Cons -> when (ta) {
                    is Empty -> None
                    is Cons -> Some(Pair(
                            f(ti.head(), ta.head()),
                            Pair(ti.tail(), ta.tail())
                    ))
                }
            }
        }

fun <A, B> Stream<A>.zipAll(
        that: Stream<B>
): Stream<Pair<Option<A>, Option<B>>> =
        unfold(Pair(this, that)) { (ti, ta) ->
            when (ti) {
                is Empty -> when (ta) {
                    is Empty -> None
                    is Cons -> Some(Pair(
                            Pair(None, Some(ta.head())),
                            Pair(ti, ta.tail())
                    ))
                }
                is Cons -> when (ta) {
                    is Empty -> Some(Pair(
                            Pair(Some(ti.head()), None),
                            Pair(ti.tail(), ta)
                    ))
                    is Cons -> Some(Pair(
                            Pair(Some(ti.head()), Some(ta.head())),
                            Pair(ti.tail(), ta.tail())
                    ))
                }

            }
        }

// 5.14
fun <A> Stream<A>.startsWith(that: Stream<A>): Boolean =
        this.zipAll(that)
                .takeWhile { !it.second.isEmpty() }
                .forAll { it.first == it.second }

// 5.15
fun <A> Stream<A>.tails(): Stream<Stream<A>> =
        unfold(this) {
            when (it) {
                is Empty -> None
                is Cons -> Some(Pair(it, it.tail()))
            }
        }

fun <A> Stream<A>.hasSubsequence(s: Stream<A>): Boolean =
        this.tails().exists { it.startsWith(s) }

// 5.16
fun <A, B> Stream<A>.scanRight(z: B, f: (A, () -> B) -> B): Stream<B> =
        foldRight({ Pair(z, Stream.of(z)) },
                { a: A, p0: () -> Pair<B, Stream<B>> ->
                    val p1: Pair<B, Stream<B>> by lazy { p0() }
                    val b2: B = f(a) { p1.first }
                    Pair<B, Stream<B>>(b2, Stream.cons({ b2 }, { p1.second }))
                }).second
