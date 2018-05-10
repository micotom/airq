package com.funglejunk.airq.logic.net

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import com.github.kittinunf.result.Result
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class AirInfoClient : AirInfoClientInterface {

    companion object {
        private const val API_ENDPOINT = "http://api.luftdaten.info/static/v1/data.json"
    }

    override fun getMeasurements(): Single<Pair<Response, Result<String, FuelError>>> {
        return API_ENDPOINT
                .httpGet()
                .rx_responseString()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
    }

}