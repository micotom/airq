package com.funglejunk.airq.util

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result

object FuelResultMapper {

    fun <T> map(fuel: Pair<Response, Result<String, FuelError>>,
                success: (String) -> T,
                failure: (FuelError) -> T): T {
        val (_, result) = fuel
        return when (result) {
            is Result.Success -> success(result.value)
            is Result.Failure -> failure(result.error)
        }
    }

}