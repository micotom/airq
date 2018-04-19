package com.funglejunk.airq.model

import java.util.*

data class StandardizedMeasurement(val date: Date, val sensorType: SensorClass, val value: Double,
                                   val coordinates: Coordinates) {

    companion object {
        val INVALID = StandardizedMeasurement(
                Calendar.getInstance().time,
                SensorClass.UNKNOWN,
                0.0,
                Coordinates(0.0, 0.0)
        )
    }

}

enum class SensorClass {
    TEMPERATURE,
    HUMIDITY,
    PM10,
    PM25,
    CO2,
    O3,
    BAROMETER,
    UNKNOWN
}

data class Coordinates(val lat: Double, val lon: Double)