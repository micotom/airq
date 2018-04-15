package com.funglejunk.airq.logic.location

import io.reactivex.Observable

interface LocationProvider {

    fun getLastKnownLocation(): Observable<Location>

}