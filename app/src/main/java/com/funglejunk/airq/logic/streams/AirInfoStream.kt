package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.net.AirInfoClientInterface
import com.funglejunk.airq.logic.parsing.AirInfoJsonParser
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.util.MeasurementFormatter
import com.funglejunk.airq.util.mapToTry
import com.funglejunk.airq.util.simpleFold
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AirInfoStream(override val location: Location,
                    private val client: AirInfoClientInterface) : ApiStream {

    private val formatter = MeasurementFormatter()

    override fun internalSingle(location: Location): Single<Try<List<Try<StandardizedMeasurement>>>> =
        Single.just(location).flatMap {
            Timber.d("start air info stream")
            client.getMeasurements()
                    .map {
                        it.mapToTry()
                    }
        }.map {
            Timber.d("received air info content")
            it.simpleFold { json ->
                AirInfoJsonParser().parse(json).fold(
                        { Try.Failure<List<AirInfoMeasurement>>(AirqException.AirInfoParser()) },
                        { Try.Success(it) }
                )
            }
        }.map {
            it.simpleFold {
                val nearbyMeasurements = it.filter {
                    val sensorLocation = Location(it.location.latitude, it.location.longitude)
                    val androidUserLocation = Location(location.latitude, location.longitude)
                    sensorLocation.distanceTo(androidUserLocation) < 2500.0f
                }
                Try.Success(nearbyMeasurements)
            }
        }.map {
            it.simpleFold {
                val locationsGrouped = it.groupBy { it.location }
                Try.Success(locationsGrouped)
            }
        }.map {
            it.simpleFold {
                val latestMeasurements = mutableListOf<Try<AirInfoMeasurement>>()
                it.entries.forEach {
                    val latest = it.value.maxBy { it.timestamp }
                    latestMeasurements.add(
                            latest?.let {
                                Try.Success(it)
                            } ?: Try.Failure(AirqException.NoStandardizedMeasurement())
                    )
                }
                Try.Success(latestMeasurements)
            }
        }.map {
            it.simpleFold {
                Try.Success(formatter.map(it))
            }
        }                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())

}