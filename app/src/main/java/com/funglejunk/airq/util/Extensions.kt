package com.funglejunk.airq.util

import io.reactivex.Single
import io.reactivex.functions.BiFunction

class Extensions {

    class String {
        companion object {
            @JvmField
            val Empty: kotlin.String = ""
        }
    }

}

fun <T: Any, V: Any> zipToPair(s1: Single<T>, s2: Single<V>): Single<Pair<T, V>> {
    return s1.zipWith(s2, BiFunction { o1Value, o2Value -> Pair(o1Value, o2Value) })
}