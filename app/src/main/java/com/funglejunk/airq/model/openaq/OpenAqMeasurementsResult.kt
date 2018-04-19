package com.funglejunk.airq.model.openaq

import com.squareup.moshi.Json

data class OpenAqMeasurementsResult(
        @Json(name = "meta") val meta: Meta,
        @Json(name = "results") val results: List<Result>
) {
    companion object {
        val NONE = OpenAqMeasurementsResult(
                Meta("", "", "", -1, -1, -1),
                emptyList<Result>()
        )
    }
}

data class Result(
        @Json(name = "location") val location: String,
        @Json(name = "parameter") val parameter: String,
        @Json(name = "date") val date: Date,
        @Json(name = "value") val value: Double,
        @Json(name = "unit") val unit: String,
        @Json(name = "coordinates") val coordinates: Coordinates,
        @Json(name = "country") val country: String,
        @Json(name = "city") val city: String
)

data class Coordinates(
        @Json(name = "latitude") val latitude: Double,
        @Json(name = "longitude") val longitude: Double
)

data class Date(
        @Json(name = "utc") val utc: String,
        @Json(name = "local") val local: String
)

data class Meta(
        @Json(name = "name") val name: String,
        @Json(name = "license") val license: String,
        @Json(name = "website") val website: String,
        @Json(name = "page") val page: Int,
        @Json(name = "limit") val limit: Int,
        @Json(name = "found") val found: Int
)