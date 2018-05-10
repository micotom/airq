package com.funglejunk.airq

import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import io.reactivex.Single
import java.net.URL
import java.util.*

class MockOpenAqClient(private val statusCode: Int, private val responseMessage: String,
                       private val result: Result<String, FuelError>) : OpenAqClientInterface {

    override fun getMeasurements(lat: Double, lon: Double, from: Date, to: Date):
            Single<Pair<Response, Result<String, FuelError>>> {
        return Single.just(
                Pair(
                        Response(
                                URL("https://api.openaq.org/v1/measurements"),
                                statusCode,
                                responseMessage
                        ),
                        result
                )
        )
    }

}