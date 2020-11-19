package fpkotlin

import kotlin.math.pow

sealed class Option<out A> {
    companion object {
        fun <A> empty(): Option<A> = None
    }
}

data class Some<out A>(val get: A) : Option<A>()
object None : Option<Nothing>()

// 4.1
fun <A, B> Option<A>.map(f: (A) -> B): Option<B> =
        when (this) {
            is None -> None
            is Some -> Some(f(get))
        }

fun <A, B> Option<A>.flatMap(f: (A) -> Option<B>): Option<B> =
        when (this) {
            is None -> None
            is Some -> f(get)
        }

fun <A> Option<A>.getOrElse(default: () -> A): A =
        when (this) {
            is None -> default()
            is Some -> get
        }

fun <A> Option<A>.orElse(ob: () -> Option<A>): Option<A> =
        when (this) {
            is None -> ob()
            is Some -> this
        }

fun <A> Option<A>.filter(f: (A) -> Boolean): Option<A> =
        when (this) {
            is None -> None
            is Some -> if (f(get)) this else None
        }

fun mean(xs: List<Double>): Option<Double> =
        when (xs) {
            is Nil -> None
            is Cons -> Some(List.sum(xs) / List.length(xs))
        }

fun variance(xs: List<Double>): Option<Double> =
        mean(xs).flatMap { m ->
            mean(List.map(xs) { x ->
                (x - m).pow(2)
            })
        }

// Applicative?
fun <A, B> lift(f: (A) -> B): (Option<A>) -> Option<B> =
        { oa -> oa.map(f) }

val absO: (Option<Double>) -> Option<Double> =
        lift { kotlin.math.abs(it) }

fun <A> catches(a: () -> A): Option<A> =
        try {
            Some(a())
        } catch (e: Throwable) {
            None
        }

// 4.3
fun <A, B, C> map2(a: Option<A>, b: Option<B>, f: (A, B) -> C): Option<C> =
        a.flatMap { va -> b.map { vb -> f(va, vb) } }

// 4.4
fun <A> sequence(xs: List<Option<A>>): Option<List<A>> =
        List.foldRightL(xs, Some(List.empty())) { x: Option<A>, acc: Option<List<A>> ->
            when (x) {
                is None -> None
                is Some -> acc.map { l -> Cons(x.get, l) }
            }
        }

// 4.5
fun <A, B> traverse(
        xa: List<A>,
        f: (A) -> Option<B>
): Option<List<B>> =
        List.foldRightL(xa, Some(List.empty())) { x: A, acc: Option<List<B>> ->
            when (val y = f(x)) {
                is None -> None
                is Some -> acc.map { l -> Cons(y.get, l) }
            }
        }

fun <A> sequenceT(xs: List<Option<A>>): Option<List<A>> =
        traverse(xs) { it }
