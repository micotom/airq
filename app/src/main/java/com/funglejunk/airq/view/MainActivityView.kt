package com.funglejunk.airq.view

import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement

interface MainActivityView {

    fun showLoadingAnimation()

    fun hideLoadingAnimation()

    fun setTemperatureValue(temperature: Double)

    fun setCo2Value(co2: Double)

    fun setPm10Value(pm10: Double)

    fun setPm25Value(pm25: Double)

    fun alphaOutIconTable()

    fun alphaInIconTable()

    @Deprecated("Using google maps view - refer to setLocations(...)")
    fun displaySensorLocations(userLocation: Location, sensorLocations: List<Location>,
                               measurements: List<StandardizedMeasurement>)

    fun clearSensorLocations()

    fun clearSensorValues()

    fun displayMeasurementOnTap(measurement: StandardizedMeasurement)

    fun hideMeasurementOnTap()

    fun setLocations(userLocation: Location, sensorLocations: List<Location>,
                     measurements: List<StandardizedMeasurement>)

}