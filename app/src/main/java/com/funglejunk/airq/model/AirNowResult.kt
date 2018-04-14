package com.funglejunk.airq.model

import com.squareup.moshi.Json

data class AirNowResult(
        @Json(name = "station") val station: Station,
        @Json(name = "data") val data: Data,
        @Json(name = "pollutant") val pollutant: Pollutant,
        @Json(name = "pollution_level") val pollutionLevel: Int
)

data class Data(
        @Json(name = "date_time") val dateTime: Int,
        @Json(name = "value") val value: Int,
        @Json(name = "pollutant") val pollutant: Int
)

data class Station(
        @Json(name = "latitude") val latitude: Double,
        @Json(name = "longitude") val longitude: Double,
        @Json(name = "station_code") val stationCode: String,
        @Json(name = "state_code") val stateCode: String,
        @Json(name = "title") val title: String
)

data class Pollutant(
        @Json(name = "unit_html") val unitHtml: String,
        @Json(name = "unit_plain") val unitPlain: String,
        @Json(name = "name") val name: String,
        @Json(name = "pollution_level") val pollutionLevel: PollutionLevel
)

data class PollutionLevel(
        @Json(name = "levels") val levels: Levels
)

data class Levels(
        @Json(name = "1") val x1: Int,
        @Json(name = "2") val x2: Int,
        @Json(name = "3") val x3: Int,
        @Json(name = "4") val x4: Int
)