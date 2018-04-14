package com.funglejunk.airq.model

import com.squareup.moshi.Json

data class AirNowCityResult(
        @Json(name = "created_at") val createdAt: Int,
        @Json(name = "name") val name: String,
        @Json(name = "slug") val slug: String
)