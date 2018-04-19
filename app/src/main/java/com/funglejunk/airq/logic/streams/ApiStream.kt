package com.funglejunk.airq.logic.streams

import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import io.reactivex.Observable
import io.reactivex.Single

interface ApiStream {

    val locationResult: StreamResult<Location>

    fun observable(): Observable<StreamResult<StandardizedMeasurement>> = Single.just(locationResult)
            .flatMapObservable {
                internalObservable(it)
            }

    fun internalObservable(locationResult: StreamResult<Location>): Observable<StreamResult<StandardizedMeasurement>>

}