package com.funglejunk.airq.logic.location

import com.funglejunk.airq.model.Location
import io.reactivex.Observable

interface LocationProvider {

    fun getLastKnownLocation(): Observable<Location>

}