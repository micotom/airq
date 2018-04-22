package com.funglejunk.airq.logic.location

import android.location.Address
import arrow.core.Option
import com.funglejunk.airq.model.Location

interface Geocoder {

    fun resolve(location: Location): Option<Address>

}