package com.funglejunk.airq.util

import android.support.v4.app.Fragment
import arrow.core.Try
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.math.BigDecimal

fun <T: Any, V: Any> zipToPair(s1: Flowable<T>, s2: Single<V>): Flowable<Pair<T, V>> {
    return s1.zipWith(s2.toFlowable(), BiFunction { o1Value, o2Value -> Pair(o1Value, o2Value) })
}

fun Double.roundTo2Decimals() = try {
    BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP)
            .toDouble()
} catch (e: NumberFormatException) {
    Timber.e("cannot round: $this")
    Double.NaN
}

fun <T> Collection<Try<T>>.filterForSuccess() = filter {
    it.isSuccess()
}.map {
    (it as Try.Success).value
}

fun Pair<Response, Result<String, FuelError>>.mapToTry(): Try<String> {
    val (_, result) = this
    return when (result) {
        is Result.Failure -> Try.Failure(result.error)
        is Result.Success -> Try.Success(result.value)
    }
}

inline fun <T, V> Try<T>.simpleFold(onSuccess: (T) -> Try<V>): Try<V> {
    return fold(
            { Try.Failure(it) },
            { onSuccess(it) }
    )
}

fun Fragment.runOnUiThread(block: () -> Unit) = activity?.runOnUiThread(block)