package com.funglejunk.airq.logic.parsing

import arrow.core.Option
import com.funglejunk.airq.model.AirNowResult
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import timber.log.Timber
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.squareup.moshi.Types.newParameterizedType
import org.json.JSONObject


class AirNowJsonParser {

    private val airNowResultListType = Types.newParameterizedType(List::class.java, AirNowResult::class.java)

    private val adapter = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<AirNowResult>>(airNowResultListType)

    fun parse(resultJson: String): Option<List<AirNowResult>> {
        return try {
            Timber.d("parsing: $resultJson")
            val list = mutableListOf<AirNowResult>()
            val json = JSONObject(resultJson)
            json.keys().forEach {
                Timber.d("key: $it -> ${json.getString(it)}")
                val result = adapter.fromJson(json.getString(it))
                result?.forEach { list.add(it) }
            }
            Option.just(list)
        } catch (e: Exception) {
            Option.empty<List<AirNowResult>>()
        }
    }

}