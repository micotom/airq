package com.funglejunk.airq.util

import android.annotation.SuppressLint
import arrow.core.Option
import com.funglejunk.airq.model.Coordinates
import com.funglejunk.airq.model.SensorClass
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.model.openaq.OpenAqResult
import java.text.SimpleDateFormat

class MeasurementFormatter {

    @SuppressLint("SimpleDateFormat")
    private val openAqDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") // TODO calc local time!
    @SuppressLint("SimpleDateFormat")
    private val airInfoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun map(measurement: OpenAqResult): Option<StandardizedMeasurement> {
        return try {
            val fm = StandardizedMeasurement(
                    openAqDateFormat.parse(measurement.date.local),
                    when (measurement.parameter) {
                        "pm25" -> SensorClass.PM25
                        "pm10" -> SensorClass.PM10
                        "o3" -> SensorClass.O3
                        "co" -> SensorClass.CO2
                        else -> throw IllegalArgumentException("Unknown sensor class: " +
                                measurement.parameter)
                    },
                    measurement.value,
                    Coordinates( measurement.coordinates.latitude, measurement.coordinates.longitude)
            )
            Option.just(fm)
        } catch (e: IllegalArgumentException) {
            Option.empty<StandardizedMeasurement>()
        }
    }

    fun map(measurement: AirInfoMeasurement): Set<StandardizedMeasurement> {
        return try {
            val measurements = mutableSetOf<StandardizedMeasurement>()
            measurement.sensorDataValues?.forEach { value ->
                val sensorClass = when(value.valueType) {
                    "P2" -> SensorClass.PM25
                    "P1" -> SensorClass.PM10
                    "humidity" -> SensorClass.HUMIDITY
                    "temperature" -> SensorClass.TEMPERATURE
                    "pressure_at_sealevel", "pressure" -> SensorClass.BAROMETER
                    else -> SensorClass.UNKNOWN
                }
                when (sensorClass) {
                    SensorClass.UNKNOWN -> {}
                    else -> {
                        val fm = StandardizedMeasurement(
                                airInfoDateFormat.parse(measurement.timestamp),
                                sensorClass,
                                value.value,
                                Coordinates(measurement.location.latitude, measurement.location.longitude)
                        )
                        measurements.add(fm)
                    }
                }
            }
            measurements
        } catch (e: Exception) {
            emptySet()
        }
    }

}