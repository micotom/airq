package com.funglejunk.airq.view

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.arsy.maps_library.MapRipple
import com.funglejunk.airq.R
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.util.runOnUiThread
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit


class MainFragment : Fragment(), MainFragmentView {

    companion object {
        private const val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    private val presenter: MainActivityPresenterInterface by inject(
            parameters = {
                mapOf(
                        "permissionListener" to RxPermissionListener(),
                        "activity" to activity!!,
                        "context" to context!!)
            }
    )
    private val sensorInfoTextHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_fragment, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapViewBundle = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        map.onCreate(mapViewBundle)

        map.getMapAsync { safeGmap ->
            try {
                val success = safeGmap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(activity, R.raw.maps_style))
                if (!success) {
                    Timber.e("Style parsing failed.")
                }
            } catch (e: Resources.NotFoundException) {
                Timber.e("Can't find style. Error: $e")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)?.let {
            it
        } ?: Bundle().apply {
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, this)
        }
        map.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewStarted(this)
        map.onResume()
    }

    override fun onPause() {
        map.getMapAsync { safeMap ->
            safeMap.clear()
        }
        presenter.viewStopped()
        super.onPause()
        map.onPause()
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

    override fun displaySensorLocations(userLocation: Location, sensorLocations: List<Location>,
                                        measurements: List<StandardizedMeasurement>) {
    }

    override fun clearSensorLocations() {}

    override fun clearSensorValues() {
        runOnUiThread {
            temperature_text.text = "-"
            co_text.text = "-"
            pm10_text.text = "-"
            pm25_text.text = "-"
        }
    }

    override fun hideMeasurementOnTap() {
        sensor_info_text.text = ""
    }

    override fun displayMeasurementOnTap(measurement: StandardizedMeasurement) {
        val sensorInfo = "${measurement.date}\n${measurement.coordinates.lat}, " +
                "${measurement.coordinates.lon}\n"
        val builder = StringBuilder().apply {
            measurement.measurements.forEach {
                append("${it.sensorType}: ${it.value}\n")
            }
        }
        sensor_info_text.text = sensorInfo + builder.toString()

        sensorInfoTextHandler.removeCallbacksAndMessages(null)
        sensorInfoTextHandler.postDelayed({
            val alphaOutAnim = AlphaAnimation(1.0f, 0.0f)
            alphaOutAnim.duration = 1000
            alphaOutAnim.fillAfter = false
            alphaOutAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    sensor_info_text.text = null
                }

                override fun onAnimationStart(p0: Animation?) {}
            })
            sensor_info_text.startAnimation(alphaOutAnim)
        }, TimeUnit.SECONDS.toMillis(5))
    }

    private val homeMarkerDescriptor: BitmapDescriptor by lazy {
        val height = 72
        val width = 72
        val bmpDrawable = ContextCompat
                .getDrawable(activity!!, R.drawable.map_home)
                as BitmapDrawable
        val b = bmpDrawable.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
        BitmapDescriptorFactory.fromBitmap(smallMarker)
    }

    private val sensorMarkerDescriptor: BitmapDescriptor by lazy {
        val height = 36
        val width = 36
        val bmpDrawable = ContextCompat
                .getDrawable(activity!!, R.drawable.baseline_trip_origin_black_18dp)
                as BitmapDrawable
        val b = bmpDrawable.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
        BitmapDescriptorFactory.fromBitmap(smallMarker)
    }

    override fun setLocations(userLocation: Location, sensorLocations: List<Location>,
                              measurements: List<StandardizedMeasurement>) {
        runOnUiThread {
            map.getMapAsync { safeMap ->
                safeMap.setMinZoomPreference(12.0f)
                val mapsCenter = LatLng(userLocation.latitude, userLocation.longitude)
                safeMap.moveCamera(CameraUpdateFactory.newLatLng(mapsCenter))

                safeMap.addMarker(
                        MarkerOptions()
                                .position(mapsCenter)
                                .icon(homeMarkerDescriptor)
                )

                sensorLocations.forEachIndexed { index, location ->
                    val markerOp = MarkerOptions()
                            .position(LatLng(location.latitude, location.longitude))
                            .icon(sensorMarkerDescriptor)
                    val marker = safeMap.addMarker(markerOp)
                    marker.alpha = 0.4f
                    marker.tag = measurements[index]
                }

                safeMap.setOnMarkerClickListener { marker ->
                    marker.alpha = 1.0f
                    val measurement = marker.tag as? StandardizedMeasurement
                    measurement?.let {
                        activity?.let { safeActivity ->
                            val mapRipple = MapRipple(
                                    safeMap,
                                    LatLng(measurement.coordinates.lat, measurement.coordinates.lon),
                                    safeActivity
                            )
                            mapRipple.withStrokeColor(
                                    ContextCompat.getColor(safeActivity, R.color.colorAccent)
                            )
                            mapRipple.withStrokewidth(20)
                            mapRipple.withTransparency(0.0f)

                            mapRipple.startRippleMapAnimation()
                            Handler().postDelayed({
                                marker.alpha = 0.4f
                                mapRipple.stopRippleMapAnimation()
                            }, 2000)
                            displayMeasurementOnTap(measurement)
                            true
                        }
                    } ?: false
                }
            }
        }
    }

    override fun onUserLocationKnown(location: Location) {
        runOnUiThread {
            map.getMapAsync { safeMap ->
                safeMap.setMinZoomPreference(12.0f)
                val mapsCenter = LatLng(location.latitude, location.longitude)
                safeMap.moveCamera(CameraUpdateFactory.newLatLng(mapsCenter))
            }
        }
    }

}