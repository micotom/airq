package com.funglejunk.airq.logic.location

import android.content.Context
import android.location.Address
import arrow.core.Option
import java.util.*

class AndroidGeocoder(private val context: Context) : Geocoder {

    override fun resolve(location: Location): Option<Address> {
        return Option.fromNullable(android.location.Geocoder(context, Locale.GERMANY).getFromLocation(
                location.latitude, location.longitude, 1)[0]
        )
    }

}