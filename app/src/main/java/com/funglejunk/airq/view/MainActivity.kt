package com.funglejunk.airq.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.funglejunk.airq.R
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.model.Location
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), MainActivityView {

    private val presenter: MainActivityPresenterInterface by inject(
            parameters = {
                mapOf(
                    "permissionListener" to RxPermissionListener(),
                    "activity" to this,
                    "context" to this)
            }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewStarted(this)
    }

    override fun onPause() {
        presenter.viewStopped()
        super.onPause()
    }

    override fun showLoadingAnimation() {
        runOnUiThread {
            loading_animation.smoothToShow()
        }
    }

    override fun hideLoadingAnimation() {
        runOnUiThread {
            loading_animation.hide()
        }
    }

    override fun setTemperatureValue(temperature: Double) {
        runOnUiThread {
            temperature_text.text = "$temperature°"
        }
    }

    override fun setCo2Value(co2: Double) {
        runOnUiThread {
            co_text.text = co2.toString()
        }
    }

    override fun setPm10Value(pm10: Double) {
        runOnUiThread {
            pm10_text.text = pm10.toString()
        }
    }

    override fun setPm25Value(pm25: Double) {
        runOnUiThread {
            pm25_text.text = pm25.toString()
        }
    }

    override fun alphaOutIconTable() {
        runOnUiThread {
            icon_table.alpha = 0.2f
        }
    }

    override fun alphaInIconTable() {
        runOnUiThread {
            icon_table.alpha = 1.0f
        }
    }

    override fun displaySensorLocations(userLocation: Location, sensorLocations: List<Location>) {
        runOnUiThread {
            sensor_map.setLocations(userLocation, sensorLocations)
        }
    }

    override fun clearSensorLocations() {
        runOnUiThread {
            sensor_map.clearLocations()
        }
    }

    override fun clearSensorValues() {
        runOnUiThread {
            temperature_text.text = "-"
            co_text.text = "-"
            pm10_text.text = "-"
            pm25_text.text = "-"
        }
    }

}
