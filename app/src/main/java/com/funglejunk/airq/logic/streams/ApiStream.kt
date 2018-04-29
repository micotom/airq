package com.funglejunk.airq.logic.streams

import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import io.reactivex.Observable
import io.reactivex.Single

interface ApiStream {

    val location: Location

    fun observable(): Observable<StreamResult<StandardizedMeasurement>> = Single.just(location)
            .flatMapObservable {
                internalObservable(it)
            }

    fun internalObservable(location: Location): Observable<StreamResult<StandardizedMeasurement>>

}