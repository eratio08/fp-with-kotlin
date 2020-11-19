package fpkotlin.ch4

import fpkotlin.ch3.Cons
import fpkotlin.ch3.List

sealed class Either<out E, out A> {
    companion object
}

data class Left<out E>(val value: E) : Either<E, Nothing>()
data class Right<out A>(val value: A) : Either<Nothing, A>()

fun <A> Either.Companion.catch(a: () -> A): Either<Exception, A> =
        try {
            Right(a())
        } catch (e: Exception) {
            Left(e)
        }

// 4.6
fun <E, A, B> Either<E, A>.map(f: (A) -> B): Either<E, B> =
        when (this) {
            is Left -> this
            is Right -> Right(f(this.value))
        }

fun <E, A, B> Either<E, A>.flatMap(f: (A) -> Either<E, B>): Either<E, B> =
        when (this) {
            is Left -> this
            is Right -> f(this.value)
        }

fun <E, A> Either<E, A>.orElse(
        f: () -> Either<E, A>
): Either<E, A> =
        when (this) {
            is Left -> f()
            is Right -> this
        }

fun <E, A, B, C> map2(
        ae: Either<E, A>,
        be: Either<E, B>,
        f: (A, B) -> C
): Either<E, C> =
        ae.flatMap { a ->
            be.map { b ->
                f(a, b)
            }
        }

// could also be done with recursion with short circuit on first error
// recursion will fail on huge stack as not tailrec optimized
fun <E, A, B> traverseE(xs: List<A>, f: (A) -> Either<E, B>): Either<E, List<B>> =
        List.foldRightL(xs, Right(List.empty())) { x: A, acc: Either<E, List<B>> ->
            acc.flatMap { l -> f(x).map { y -> Cons(y, l) } }
        }


sealed class Partial<out A, out B>
sealed class Failures<out A>(val get: List<A>): Partial<A, Nothing>()
sealed class Success<out B>(val get: B): Partial<Nothing, B>()