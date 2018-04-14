package com.funglejunk.airq.logic.net

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import com.github.kittinunf.result.Result
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class AirNowClient : AirNowClientInterface {

    override fun getCityList(): Single<Pair<Response, Result<String, FuelError>>> {
        return "https://luft.jetzt/api/city?_format=json"
                .httpGet()
                .rx_responseString()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
    }

    override fun getDataBySlug(slug: String): Single<Pair<Response, Result<String, FuelError>>> {
        return "https://luft.jetzt/api/$slug?_format=json"
                .httpGet()
                .rx_responseString()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
    }

}