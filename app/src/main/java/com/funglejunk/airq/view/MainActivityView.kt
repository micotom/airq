package com.funglejunk.airq.view

import com.funglejunk.airq.model.Location

interface MainActivityView {

    fun showLoadingAnimation()

    fun hideLoadingAnimation()

    fun setTemperatureValue(temperature: Double)

    fun setCo2Value(co2: Double)

    fun setPm10Value(pm10: Double)

    fun setPm25Value(pm25: Double)

    fun alphaOutIconTable()

    fun alphaInIconTable()

    fun displaySensorLocations(userLocation: Location, sensorLocations: List<Location>)

    fun clearSensorLocations()

}