package com.funglejunk.airq.model.airinfo

import com.squareup.moshi.Json

data class AirInfoMeasurement(
        @Json(name = "id") val id: Int,
        @Json(name = "timestamp") val timestamp: String,
        @Json(name = "location") val location: Location,
        @Json(name = "sensor") val sensor: Sensor,
        @Json(name = "sensordatavalues") val sensorDataValues: List<SensorDataValue>?
)

data class Location(
        @Json(name = "id") val id: Int,
        @Json(name = "latitude") val latitude: Double,
        @Json(name = "longitude") val longitude: Double,
        @Json(name = "altitude") val altitude: String,
        @Json(name = "country") val country: String
)

data class SensorDataValue(
        @Json(name = "id") val id: Long?,
        @Json(name = "value") val value: Double,
        @Json(name = "value_type") val valueType: String
)

data class Sensor(
        @Json(name = "id") val id: Int,
        @Json(name = "pin") val pin: String,
        @Json(name = "sensor_type") val sensorType: SensorType
)

data class SensorType(
        @Json(name = "id") val id: Int,
        @Json(name = "name") val name: String,
        @Json(name = "manufacturer") val manufacturer: String
)