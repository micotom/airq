package com.funglejunk.airq.logic.parsing

import arrow.core.Option
import com.funglejunk.airq.model.openaq.OpenAqMeasurementsResult
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import timber.log.Timber

class OpenAqMeasurementsResultParser {

    private val adapter = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<OpenAqMeasurementsResult>(OpenAqMeasurementsResult::class.java)

    fun parse(resultJson: String): Option<OpenAqMeasurementsResult> {
        return try {
            Timber.d("parsing: $resultJson")
            adapter.fromJson(resultJson)?.let {
                Option.just(it)
            } ?: Option.empty()
        } catch (e: Exception) {
            Option.empty()
        }
    }



}