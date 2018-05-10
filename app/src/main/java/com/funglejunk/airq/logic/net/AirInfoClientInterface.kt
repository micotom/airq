package com.funglejunk.airq.logic.net

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import io.reactivex.Single

interface AirInfoClientInterface {

    fun getMeasurements(): Single<Pair<Response, Result<String, FuelError>>>

}