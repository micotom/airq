package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import io.reactivex.Single

interface ApiStream {

    val location: Location

    fun single(): Single<Try<List<Try<StandardizedMeasurement>>>> = Single.just(location)
            .flatMap {
                internalSingle(it)
            }

    fun internalSingle(location: Location): Single<Try<List<Try<StandardizedMeasurement>>>>

}