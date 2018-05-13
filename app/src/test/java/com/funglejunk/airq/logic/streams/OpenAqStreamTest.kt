package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.MockOpenAqClient
import com.funglejunk.airq.model.Coordinates
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.github.kittinunf.result.Result
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OpenAqStreamTest {

    private val successJson = """
{
   "meta":{
      "name":"openaq-api",
      "license":"CC BY 4.0",
      "website":"https://docs.openaq.org/",
      "page":1,
      "limit":100,
      "found":8214
   },
   "results":[
      {
         "location":"DEBY115",
         "parameter":"no2",
         "date":{
            "utc":"2018-05-10T07:00:00.000Z",
            "local":"2018-05-10T09:00:00+02:00"
         },
         "value":63.99,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.149606,
            "longitude":11.536513
         },
         "country":"DE",
         "city":"Bayern"
      },
      {
         "location":"DEBY039",
         "parameter":"o3",
         "date":{
            "utc":"2018-05-10T07:00:00.000Z",
            "local":"2018-05-10T09:00:00+02:00"
         },
         "value":94.21,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.154534,
            "longitude":11.554669
         },
         "country":"DE",
         "city":"Bayern"
      },
      {
         "location":"DEBY039",
         "parameter":"o3",
         "date":{
            "utc":"2018-05-10T06:00:00.000Z",
            "local":"2018-05-10T08:00:00+02:00"
         },
         "value":72.32,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.154534,
            "longitude":11.554669
         },
         "country":"DE",
         "city":"Bayern"
      },
      {
         "location":"DEBY115",
         "parameter":"pm25",
         "date":{
            "utc":"2018-05-10T06:00:00.000Z",
            "local":"2018-05-10T08:00:00+02:00"
         },
         "value":6.89,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.149606,
            "longitude":11.536513
         },
         "country":"DE",
         "city":"Bayern"
      },
      {
         "location":"DEBY115",
         "parameter":"pm10",
         "date":{
            "utc":"2018-05-10T06:00:00.000Z",
            "local":"2018-05-10T08:00:00+02:00"
         },
         "value":13.41,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.149606,
            "longitude":11.536513
         },
         "country":"DE",
         "city":"Bayern"
      },
      {
         "location":"DEBY115",
         "parameter":"pm10",
         "date":{
            "utc":"2018-05-10T04:00:00.000Z",
            "local":"2018-05-10T06:00:00+02:00"
         },
         "value":14.73,
         "unit":"µg/m³",
         "coordinates":{
            "latitude":48.149606,
            "longitude":11.536513
         },
         "country":"DE",
         "city":"Bayern"
      }
   ]
}
        """

    private lateinit var observer: TestObserver<Try<StandardizedMeasurement>>

    @Before
    fun before() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        observer = TestObserver()
    }

    @After
    fun after() {
        observer.dispose()
    }

    @Test
    fun test() {
        val client = MockOpenAqClient(200, successJson, Result.Success(successJson))
        val stream = OpenAqStream(Location(2.0, 1.0), client)

        observer = TestObserver()
        stream.observable().subscribe(observer)
        observer.await()
        val results = observer.values()

        // two distinct stations
        assertEquals(2, observer.valueCount())

        // parsing successful
        assertTrue(results.all { it is Try.Success })
        val validatedResults = results.map { (it as Try.Success).value }

        // find first station
        assertTrue(validatedResults.find {
            it.coordinates == Coordinates(48.149606, 11.536513)
        }  != null)

        // find second station
        assertTrue(validatedResults.find {
            it.coordinates == Coordinates(48.154534, 11.554669)
        }  != null)

        // check measurements
        val firstStation = validatedResults.find {
            it.coordinates == Coordinates(48.149606, 11.536513)
        }!!
        assertEquals(3, firstStation.measurements.size)
    }

}