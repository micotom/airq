package com.funglejunk.airq.logic.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val subject = PublishSubject.create<Location>()

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Observable<Location> {
        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
            Timber.d("received new location")
            subject.onNext(Location(it.latitude, it.longitude))
        }
        return subject.hide()
    }

}