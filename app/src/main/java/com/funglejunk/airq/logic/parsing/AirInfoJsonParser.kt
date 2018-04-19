package com.funglejunk.airq.logic.parsing

import arrow.core.Option
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber

class AirInfoJsonParser {

    private val airInfoResultListType = Types.newParameterizedType(List::class.java, AirInfoMeasurement::class.java)

    private val adapter = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<AirInfoMeasurement>>(airInfoResultListType)

    fun parse(resultJson: String): Option<List<AirInfoMeasurement>> {
        return try {
            //Timber.d("parsing: $resultJson")
            adapter.fromJson(resultJson)?.let {
                Option.just(it)
            } ?: Option.empty<List<AirInfoMeasurement>>()
        } catch (e: Exception) {
            Timber.d("parsing failed: ${e.message}")
            Option.empty<List<AirInfoMeasurement>>()
        }
    }

}