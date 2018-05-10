package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.MockOpenAqClient
import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.github.kittinunf.result.Result
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext.closeKoin
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject
import org.koin.test.KoinTest

class OpenAqStreamTest : KoinTest {

    private val module: Module = applicationContext {
        bean {
            MockOpenAqClient(200, "Foo",
                    Result.Success("Bar")) as OpenAqClientInterface
        }
    }

    @Before
    fun before(){
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        startKoin(listOf(module))
    }

    @After
    fun after(){
        closeKoin()
    }

    @Test
    fun test() {
        val client: OpenAqClientInterface by inject()
        val stream = OpenAqStream(Location(2.0, 1.0), client)

        val observer = TestObserver<Try<StandardizedMeasurement>>()
        stream.observable().subscribe(observer)

        observer.assertComplete()
    }

}