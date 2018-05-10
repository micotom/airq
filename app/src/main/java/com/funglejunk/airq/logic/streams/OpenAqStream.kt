package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.funglejunk.airq.logic.parsing.OpenAqMeasurementsResultParser
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.openaq.OpenAqMeasurementsResult
import com.funglejunk.airq.util.FuelResultMapper
import com.funglejunk.airq.util.MeasurementFormatter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class OpenAqStream(override val location: Location,
                   private val openAqClient: OpenAqClientInterface) : ApiStream {

    private val formatter = MeasurementFormatter()

    override fun internalObservable(location: Location): Observable<Try<StandardizedMeasurement>> {
        Timber.d("start open aq stream")

        return Single.just(location).flatMap {
            val now = Calendar.getInstance().time
            val oneHourBefore = Date(System.currentTimeMillis() - (48 * 3600 * 1000))
            openAqClient.getMeasurements(
                    location.latitude, location.longitude, oneHourBefore, now
            ).map {
                FuelResultMapper.map(it,
                        { Try.Success(it) },
                        { Try.Failure<String>(it.exception) }
                )
            }
        }.map {
            it.fold(
                    { Try.Failure<OpenAqMeasurementsResult>(it) },
                    { json ->
                        OpenAqMeasurementsResultParser().parse(json)
                                .fold(
                                        { Try.Failure<OpenAqMeasurementsResult>(AirqException.OpenAqParser()) },
                                        { Try.Success(it) }
                                )
                    }
            )
        }.toObservable().map {
            it.fold(
                    {
                        Try.Failure<List<Try<StandardizedMeasurement>>>(it)
                    },
                    {
                        Try.Success(formatter.map(it))
                    }
            )
        }.flatMapIterable {
            it.fold(
                    { emptyList<Try<StandardizedMeasurement>>() },
                    { it }
            )
        }.observeOn(Schedulers.io()).subscribeOn(Schedulers.io())

    }

}