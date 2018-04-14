package com.funglejunk.airq.logic.location

import android.location.Address
import arrow.core.Option

interface Geocoder {

    fun resolve(location: Location): Option<Address>

}