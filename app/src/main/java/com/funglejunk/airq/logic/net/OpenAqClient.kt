package com.funglejunk.airq.logic.net

import android.annotation.SuppressLint
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import com.github.kittinunf.result.Result
import io.reactivex.Single
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class OpenAqClient : OpenAqClientInterface {

    // 2015-12-20T09:00:00
    @SuppressLint("SimpleDateFormat")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    override fun getMeasurements(lat: Double, lon: Double, from: Date, to: Date):
            Single<Pair<Response, Result<String, FuelError>>> {
        val fromDate = dateFormatter.format(from)
        val toDate = dateFormatter.format(to)
        val requestUri = "https://api.openaq.org/v1/measurements?" +
                "coordinates=$lat,$lon&" +
                "date_from=$fromDate&" +
                "date_to=$toDate"
        Timber.d("request: $requestUri")
        return requestUri.httpGet().rx_responseString()
    }

}