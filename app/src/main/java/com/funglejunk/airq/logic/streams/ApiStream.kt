package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import io.reactivex.Observable
import io.reactivex.Single

interface ApiStream {

    val location: Location

    fun observable(): Observable<Try<StandardizedMeasurement>> = Single.just(location)
            .flatMapObservable {
                internalObservable(it)
            }

    fun internalObservable(location: Location): Observable<Try<StandardizedMeasurement>>

}