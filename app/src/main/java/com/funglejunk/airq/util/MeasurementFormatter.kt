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

    private val openAqDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX",
            Locale.getDefault())
    private val airInfoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault())

    fun map(openAqMeasurements: OpenAqMeasurementsResult): List<Try<StandardizedMeasurement>> {

        val groupedByStations = openAqMeasurements.results.groupBy { it.location }
        val groupedByLocationAndSensorClass = groupedByStations.entries.map {
            it.key to (it.value.groupBy { it.parameter })
        }
        val latestMeasurements = mutableMapOf<OpenAqCoordinates, List<OpenAqResult>>()
        groupedByLocationAndSensorClass.forEach { (_, sensorMeasurementMaps) ->
            val latMs = mutableListOf<OpenAqResult>()
            sensorMeasurementMaps.entries.forEach {
                val latest = it.value.sortedBy { it.date.local }.last()
                latMs.add(latest)
            }
            latestMeasurements[latMs.first().coordinates] = latMs
        }

        return latestMeasurements.entries.fold(mutableListOf()) { retList, entry ->
            val coordinates = entry.key
            val resultList = entry.value
            val measurements = resultList.map { result ->
                val sensor = when (result.parameter) {
                    "pm25" -> SensorClass.PM25
                    "pm10" -> SensorClass.PM10
                    "o3" -> SensorClass.O3
                    "co" -> SensorClass.CO2
                    "no2" -> SensorClass.NO2
                    else -> SensorClass.UNKNOWN
                }
                when (sensor) {
                    SensorClass.UNKNOWN -> {
                        Timber.w("unknown sensor value: ${result.parameter}")
                        null
                    }
                    else -> {
                        val value = result.value
                        Measurement(sensor, value)
                    }
                }
            }.filterNotNull()
            val latest = resultList.sortedBy { it.date.local }.last()
            retList.add(Try.Success(
                    StandardizedMeasurement(
                            openAqDateFormat.parse(latest.date.local),
                            measurements,
                            Coordinates(coordinates.latitude, coordinates.longitude),
                            ApiSource.OPEN_AQ
                    ))
            )
            retList
        }
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