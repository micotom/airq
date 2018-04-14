package com.funglejunk.airq.logic.location

import io.reactivex.Observable
import io.reactivex.Single

interface LocationProvider {

    fun getLastKnownLocation(): Observable<Location>

}