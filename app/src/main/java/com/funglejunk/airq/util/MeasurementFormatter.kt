package com.funglejunk.airq.util

import arrow.core.Try
import com.funglejunk.airq.model.*
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.model.openaq.OpenAqCoordinates
import com.funglejunk.airq.model.openaq.OpenAqMeasurementsResult
import com.funglejunk.airq.model.openaq.OpenAqResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class MeasurementFormatter {

    private val openAqDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",
            Locale.getDefault())
    private val airInfoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault())

    fun map(openAqMeasurements: OpenAqMeasurementsResult): List<Try<StandardizedMeasurement>> {
        val groupedByLocations = openAqMeasurements.results.groupBy { it.coordinates }

        val groupedByLocationAndSensorClass = groupedByLocations.entries.map {
            it.key to (it.value.groupBy { it.parameter })
        }
        val latestMeasurements = mutableMapOf<OpenAqCoordinates, List<OpenAqResult>>()
        groupedByLocationAndSensorClass.forEach { (coordinates, measurements) ->
            val latMs = mutableListOf<OpenAqResult>()

            measurements.entries.forEach {
                val latest = it.value.sortedBy { it.date.local }.last()
                latMs.add(latest)
            }
            latestMeasurements[coordinates] = latMs
        }

        val returnList = mutableListOf<Try<StandardizedMeasurement>>()
        latestMeasurements.entries.forEach {
            val coordinates = it.key
            val measurements = it.value
            val latestDate = measurements.first().date
            val stdMs = mutableListOf<Measurement>()
            measurements.forEach {
                measurements.map {
                    val sensor = when (it.parameter) {
                        "pm25" -> SensorClass.PM25
                        "pm10" -> SensorClass.PM10
                        "o3" -> SensorClass.O3
                        "co" -> SensorClass.CO2
                        else -> SensorClass.UNKNOWN
                    }
                    when (sensor) {
                        SensorClass.UNKNOWN -> Timber.w("unknown sensor value: ${it.parameter}")
                        else -> {
                            val value = it.value
                            stdMs.add(Measurement(sensor, value))
                        }
                    }
                }
            }
            returnList.add(
                    Try.Success(
                            StandardizedMeasurement(
                                    openAqDateFormat.parse(latestDate.local),
                                    stdMs,
                                    Coordinates(coordinates.latitude, coordinates.longitude),
                                    ApiSource.OPEN_AQ
                            )
                    )
            )
        }

        return returnList

        /*
        val returnList = mutableListOf<Try<StandardizedMeasurement>>()

        latestMeasurements.forEach { (location, results) ->
            val measurements = mutableListOf<Measurement>()
            results.forEach { result ->
                val sensor = when (result.parameter) {
                    "pm25" -> SensorClass.PM25
                    "pm10" -> SensorClass.PM10
                    "o3" -> SensorClass.O3
                    "co" -> SensorClass.CO2
                    else -> SensorClass.UNKNOWN
                }
                when (sensor) {
                    SensorClass.UNKNOWN -> Timber.w("unknown sensor value: ${result.parameter}")
                    else -> {
                        val value = result.value
                        measurements.add(Measurement(sensor, value))
                    }
                }
            }
            val newest = results.maxBy { it.date.local }
            returnList.add(
                    newest?.let {
                        val coordinates = Coordinates(location.latitude, location.longitude)
                        val formattedDate = openAqDateFormat.parse(newest.date.local)
                        Try.Success(
                                StandardizedMeasurement(formattedDate, measurements, coordinates,
                                        ApiSource.OPEN_AQ)
                        )
                    } ?: Try.Failure(AirqException.NoOpenAqDateMeasurement())
            )
        }



        groupedByLocations.entries.forEach { (location, results) ->
            val measurements = mutableListOf<Measurement>()
            results.forEach { result ->
                val sensor = when (result.parameter) {
                    "pm25" -> SensorClass.PM25
                    "pm10" -> SensorClass.PM10
                    "o3" -> SensorClass.O3
                    "co" -> SensorClass.CO2
                    else -> SensorClass.UNKNOWN
                }
                when (sensor) {
                    SensorClass.UNKNOWN -> Timber.w("unknown sensor value: ${result.parameter}")
                    else -> {
                        val value = result.value
                        measurements.add(Measurement(sensor, value))
                    }
                }
            }
            val newest = results.maxBy { it.date.local }
            returnList.add(
                    newest?.let {
                        val coordinates = Coordinates(location.latitude, location.longitude)
                        val formattedDate = openAqDateFormat.parse(newest.date.local)
                        Try.Success(
                                StandardizedMeasurement(formattedDate, measurements, coordinates,
                                        ApiSource.OPEN_AQ)
                        )
                    } ?: Try.Failure(AirqException.NoOpenAqDateMeasurement())
            )
        }

        return returnList
        */
    }

    fun map(measurements: List<Try<AirInfoMeasurement>>): List<Try<StandardizedMeasurement>> {
        return measurements.map {
            it.fold(
                    {
                        Try.Failure<StandardizedMeasurement>(it)
                    },
                    { measurement ->
                        val date = airInfoDateFormat.parse(measurement.timestamp)
                        val coordinates = Coordinates(
                                measurement.location.latitude, measurement.location.longitude
                        )
                        val values = measurement.sensorDataValues?.map {
                            val sensorType = when (it.valueType) {
                                "P2" -> SensorClass.PM25
                                "P1" -> SensorClass.PM10
                                "humidity" -> SensorClass.HUMIDITY
                                "temperature" -> SensorClass.TEMPERATURE
                                "pressure_at_sealevel", "pressure" -> SensorClass.BAROMETER
                                else -> SensorClass.UNKNOWN
                            }
                            val value = it.value
                            Measurement(sensorType, value)
                        }
                        when (values) {
                            null -> Try.Failure<StandardizedMeasurement>(
                                    AirqException.NoAirInfoMeasurement()
                            )
                            else -> Try.Success(StandardizedMeasurement(
                                    date, values, coordinates, ApiSource.AIR_INFO)
                            )
                        }
                    }
            )
        }
    }

}