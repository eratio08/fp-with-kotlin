package fpkotlin.ch3


sealed class List<out A> {
    companion object {
        fun <A> of(vararg aa: A): List<A> {
            val tail = aa.sliceArray(1 until aa.size)
            return when {
                aa.isEmpty() -> Nil
                else -> Cons(aa[0], of(*tail))
            }
        }

        fun sum(xs: List<Int>): Int =
                when (xs) {
                    is Nil -> 0
                    is Cons -> xs.head + sum(xs.tail)
                }

        fun product(xs: List<Double>): Double =
                when (xs) {
                    is Nil -> 1.0
                    is Cons -> xs.head * product(xs.tail)
                }

        fun <A> tail(list: List<A>): List<A> =
                when (list) {
                    is Nil -> Nil
                    is Cons -> list.tail
                }

        fun <A> setHead(xs: List<A>, x: A): List<A> =
                when (xs) {
                    is Nil -> Cons(x, Nil)
                    is Cons -> Cons(x, xs.tail)
                }

        fun <A> drop(l: List<A>, n: Int): List<A> {
            tailrec fun loop(n: Int, rest: List<A>): List<A> =
                    when {
                        n == 0 -> rest
                        rest is Nil -> Nil
                        else -> {
                            rest as Cons<A>
                            loop(n - 1, rest.tail)
                        }
                    }

            return loop(n, l)
        }

        tailrec fun <A> dropWhile(l: List<A>, f: (A) -> Boolean): List<A> =
                when (l) {
                    is Nil -> Nil
                    is Cons -> if (f(l.head)) dropWhile(l.tail, f) else l.tail
                }

        fun <A> append(a1: List<A>, a2: List<A>): List<A> =
                when (a1) {
                    is Nil -> a2
                    is Cons -> Cons(a1.head, append(a1.tail, a2))
                }

        fun <A> init(l: List<A>): List<A> {
            tailrec fun loop(l: List<A>, res: List<A>): List<A> =
                    when (l) {
                        is Nil -> Nil
                        is Cons -> {
                            if (l.tail is Cons && l.tail.tail is Nil) append(res, Cons(l.head, Nil))
                            else loop(l.tail, append(res, Cons(l.head, Nil)))
                        }
                    }

            return loop(l, Nil)
        }

        fun <A, B> foldRight(xs: List<A>, z: B, f: (A, B) -> B): B =
                when (xs) {
                    is Nil -> z
                    is Cons -> f(xs.head, foldRight(xs.tail, z, f))
                }

        // 3.6
        fun sum2(ints: List<Int>): Int =
                foldRight(ints, 0) { a, b -> a + b }

        fun product2(doubles: List<Double>): Double =
                foldRight(doubles, 1.0) { a, b -> a * b }

        // 3.7
        fun <A> empty(): List<A> = Nil

        // 3.8
        fun <A> length(xs: List<A>): Int =
                foldRight(xs, 0) { _, acc -> acc + 1 }

        // 3.9
        tailrec fun <A, B> foldLeft(xs: List<A>, z: B, f: (B, A) -> B): B =
                when (xs) {
                    is Nil -> z
                    is Cons -> foldLeft(xs.tail, f(z, xs.head), f)
                }

        // 3.10
        fun sumL(ints: List<Int>): Int =
                foldLeft(ints, 0) { sum, a -> sum + a }

        fun productL(doubles: List<Double>): Double =
                foldLeft(doubles, 1.0) { prod, a -> prod * a }

        // 3.11
        fun <A> reverse(xs: List<A>): List<A> =
                foldLeft(xs, empty()) { acc, a -> Cons(a, acc) }

        // 3.12
        fun <A, B> foldLeftR(xs: List<A>, z: B, f: (B, A) -> B): B =
                foldRight(xs,
                        { b: B -> b },
                        { a, g ->
                            { b -> g(f(b, a)) }
                        })(z)

        fun <A, B> foldRightL(xs: List<A>, z: B, f: (A, B) -> B): B =
                foldLeft(xs,
                        { b: B -> b },
                        { g, a ->
                            { b -> g(f(a, b)) }
                        })(z)

        fun <A, B> foldLeftRDemystified(xs: List<A>, acc: B, combiner: (B, A) -> B): B {
            val identity: Identity<B> = { b: B -> b }
            val combinerDelayer: (A, Identity<B>) -> Identity<B> =
                    { a: A, delayedExec: Identity<B> ->
                        { b: B -> delayedExec(combiner(b, a)) }
                    }
            val chain: Identity<B> = foldRight(xs, identity, combinerDelayer)
            return chain(acc)
        }

        // 3.13
        fun <A> appendR(xs: List<A>, ys: List<A>): List<A> =
                foldRightL(xs, ys) { a, acc -> Cons(a, acc) }

        fun <A> appendL(xs: List<A>, ys: List<A>): List<A> =
                foldLeft(xs,
                        { b: List<A> -> b },
                        { g, a ->
                            { b ->
                                g(Cons(a, b))
                            }
                        })(ys)

        // 3.14
        fun <A> concat(xss: List<List<A>>): List<A> =
                foldLeft(xss, empty()) { xs, acc ->
                    appendL(xs, acc)
                }

        // 3.15
        fun inc(xs: List<Int>): List<Int> =
                foldRightL(xs, empty()) { a, acc -> Cons(a + 1, acc) }

        // 3.17
        fun <A, B> map(xs: List<A>, f: (A) -> B): List<B> =
                foldRightL(xs, empty()) { a, acc -> Cons(f(a), acc) }

        // 3.18
        fun <A> filter(xs: List<A>, p: (A) -> Boolean): List<A> =
                foldRightL(xs, empty()) { a, acc -> if (p(a)) Cons(a, acc) else acc }

        // 3.19
        fun <A, B> flatMap(xs: List<A>, f: (A) -> List<B>): List<B> =
                foldRightL(xs, empty()) { a, acc -> appendL(f(a), acc) }

        // 3.20
        fun <A> filterF(xs: List<A>, p: (A) -> Boolean): List<A> =
                flatMap(xs) { a: A -> if (p(a)) Cons(a, Nil) else Nil }

        // 3.21
        fun sumE(xs: List<Int>, ys: List<Int>): List<Int> =
                when (xs) {
                    is Nil -> Nil
                    is Cons -> when (ys) {
                        is Nil -> Nil
                        is Cons -> Cons(xs.head + ys.head, sumE(xs.tail, ys.tail))
                    }
                }

        // 3.22
        fun <A, B, C> zipWith(xs: List<A>, ys: List<B>, f: (A, B) -> C): List<C> =
                when (xs) {
                    is Nil -> Nil
                    is Cons -> when (ys) {
                        is Nil -> Nil
                        is Cons -> Cons(f(xs.head, ys.head), zipWith(xs.tail, ys.tail, f))
                    }
                }

        // 3.23
        tailrec fun <A> startsWith(l1: List<A>, l2: List<A>): Boolean =
                when (l1) {
                    is Nil -> l2 == Nil
                    is Cons -> when (l2) {
                        is Nil -> true
                        is Cons ->
                            if (l1.head == l2.head)
                                startsWith(l1.tail, l2.tail)
                            else false
                    }
                }

        tailrec fun <A> hasSubsequence(xs: List<A>, sub: List<A>): Boolean =
                when (xs) {
                    is Nil -> false
                    is Cons -> if (startsWith(xs, sub)) true
                    else hasSubsequence(xs.tail, sub)
                }

        // 4

        fun sum(xs: List<Double>): Double =
                foldLeft(xs, 0.0) { acc, i -> acc + i }
    }
}

typealias Identity<B> = (B) -> B

object Nil : List<Nothing>()
data class Cons<out A>(
        val head: A,
        val tail: List<A>,
) : List<A>()

// 3.5 Trees

sealed class Tree<out A> {
    companion object {
        // 3.24
        fun <A> size(t: Tree<A>): Int =
                when (t) {
                    is Leaf -> 1
                    is Branch -> 1 + size(t.left) + size(t.right)
                }

        // 3.25
        fun maximum(t: Tree<Int>): Int =
                when (t) {
                    is Leaf -> t.value
                    is Branch -> maxOf(maximum(t.left), maximum(t.right))
                }

        // 3.26
        fun depth(t: Tree<Int>): Int =
                when (t) {
                    is Leaf -> 1
                    is Branch -> 1 + maxOf(depth(t.left), depth(t.right))
                }

        // 3.27
        fun <A, B> map(t: Tree<A>, f: (A) -> B): Tree<B> =
                when (t) {
                    is Leaf -> Leaf(f(t.value))
                    is Branch -> Branch(map(t.left, f), map(t.right, f))
                }

        // 3.28
        fun <A, B> fold(ta: Tree<A>, l: (A) -> B, b: (B, B) -> B): B =
                when (ta) {
                    is Leaf -> l(ta.value)
                    is Branch -> b(fold(ta.left, l, b), fold(ta.right, l, b))
                }

        fun <A> sizeF(ta: Tree<A>): Int =
                fold(ta, { 1 }, { a1, a2 -> a1 + a2 })

        fun maximumF(ta: Tree<Int>): Int =
                fold(ta, { a -> a }, { a1, a2 -> maxOf(a1, a2) })

        fun <A> depthF(ta: Tree<A>): Int =
                fold(ta, { 1 }, { a1, a2 -> 1 + maxOf(a1, a2) })

        fun <A, B> mapF(ta: Tree<A>, f: (A) -> B): Tree<B> =
                fold(
                        ta,
                        { a -> Leaf(f(a)) },
                        { tl: Tree<B>, tr: Tree<B> -> Branch(tl, tr) }
                )
    }
}

data class Leaf<A>(val value: A) : Tree<A>()
data class Branch<A>(val left: Tree<A>, val right: Tree<A>) : Tree<A>()

fun main() {
    println(Tree.depth(Branch(Branch(Leaf(1), Leaf(2)), Leaf(3))))
}