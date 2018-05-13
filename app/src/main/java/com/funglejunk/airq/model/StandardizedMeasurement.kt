package com.funglejunk.airq.model

import java.util.*

data class StandardizedMeasurement(val date: Date, val measurements: List<Measurement>,
                                   val coordinates: Coordinates, val source: ApiSource)

data class Measurement(val sensorType: SensorClass, val value: Double)

enum class SensorClass {
    TEMPERATURE,
    HUMIDITY,
    PM10,
    PM25,
    CO2,
    O3,
    BAROMETER,
    NO2,
    UNKNOWN
}

data class Coordinates(val lat: Double, val lon: Double)

enum class ApiSource {
    OPEN_AQ,
    AIR_INFO
}