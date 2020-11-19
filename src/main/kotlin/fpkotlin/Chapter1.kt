package fpkotlin

import java.math.BigDecimal

class Cafe {
    fun buyCoffee(cc: CreditCard): Pair<Coffee, Charge> = TODO()

    fun buyCoffees(
            cc: CreditCard,
            n: Int
    ): Pair<kotlin.collections.List<Coffee>, Charge> {
        val purchases = List(n) { buyCoffee(cc) }
        val (coffees, charges) = purchases.unzip()
        return coffees to charges.reduce { c1, c2 -> c1 + c2 }
    }
}

data class Coffee(val price: BigDecimal)

data class CreditCard(val brand: String)

data class Charge(val cc: CreditCard, val amount: Float) {
    private fun combine(other: Charge): Charge =
            if (cc == other.cc)
                Charge(cc, amount + other.amount)
            else throw Exception(
                    "Cannot combine charges of different cards"
            )

    operator fun plus(other: Charge): Charge = combine(other)
}

fun List<Charge>.coalesce(): kotlin.collections.List<Charge> =
        this.groupBy { it.cc }.values
                .map { it.reduce { a, b -> a + b } }

fun factorial(i: Int): Int {
    tailrec fun go(n: Int, acc: Int): Int =
            if (n <= 0) acc
            else go(n - 1, n * acc)
    return go(i, 1)
}