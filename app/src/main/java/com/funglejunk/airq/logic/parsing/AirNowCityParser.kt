package com.funglejunk.airq.logic.parsing

import arrow.core.Option
import com.funglejunk.airq.model.AirNowCityResult
import com.funglejunk.airq.model.AirNowResult
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber

class AirNowCityParser {

    var airNowResultListType = Types.newParameterizedType(List::class.java,
            AirNowCityResult::class.java)

    private val adapter = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<AirNowCityResult>>(airNowResultListType)

    fun parse(resultJson: String): Option<List<AirNowCityResult>> {
        return try {
            Timber.d("parsing: $resultJson")
            adapter.fromJson(resultJson)?.let {
                Option.just(it)
            } ?: Option.empty<List<AirNowCityResult>>()
        } catch (e: Exception) {
            Option.empty<List<AirNowCityResult>>()
        }
    }

}