package com.funglejunk.airq.logic.net

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import io.reactivex.Single
import java.util.*

interface OpenAqClientInterface {

    fun getMeasurements(lat: Double, lon: Double, from: Date, to: Date) :
            Single<Pair<Response, Result<String, FuelError>>>

}