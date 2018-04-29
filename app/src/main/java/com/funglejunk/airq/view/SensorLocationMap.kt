package com.funglejunk.airq.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.funglejunk.airq.R
import com.funglejunk.airq.logic.location.MercatorProjector
import com.funglejunk.airq.model.Location
import timber.log.Timber


class SensorLocationMap @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    companion object {
        private const val scale = 5000.0
    }

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryLight)
    }

    private val secPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorDivider)
    }

    private val outlinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
    }

    private var userLocationPixels: MercatorProjector.MercatorPoint? = null
    private var sensorLocationPixels = emptyList<MercatorProjector.MercatorPoint>()

    private var viewHeight: Int? = null
    private var viewWidth: Int? = null

    private var currentPointRadius = 20f
    private var sensorsDrawn = false

    private var animationHandler = Handler()
    private val animationUpdateDelayMs = 125L

    private val animationRunnable = object : Runnable {
        override fun run() {
            currentPointRadius = when (currentPointRadius) {
                30f -> 20f
                else -> currentPointRadius + 1
            }
            if (sensorsDrawn) {
                viewWidth?.let { safeWidth ->
                    viewHeight?.let { safeHeight ->
                        val centerWidth = safeWidth / 2.0
                        val centerHeight = safeHeight / 2.0
                        invalidate((centerWidth - (currentPointRadius + 1) / 2.0).toInt(),
                                (centerHeight - (currentPointRadius + 1) / 2.0).toInt(),
                                (centerWidth + (currentPointRadius + 1) / 2.0).toInt(),
                                (centerHeight + (currentPointRadius + 1) / 2.0).toInt()
                        )
                    }
                }
            }
            animationHandler.postDelayed(this, animationUpdateDelayMs)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animationHandler.postDelayed(animationRunnable, animationUpdateDelayMs)
    }

    override fun onDetachedFromWindow() {
        animationHandler.removeCallbacks(animationRunnable)
        sensorsDrawn = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
        viewWidth = w
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            userLocationPixels?.let { safeUserLocation ->
                canvas.drawCircle(safeUserLocation.x.toFloat(), safeUserLocation.y.toFloat(),
                        currentPointRadius + 2.0f, outlinePaint)
                canvas.drawCircle(safeUserLocation.x.toFloat(), safeUserLocation.y.toFloat(),
                        currentPointRadius, secPaint)
                sensorLocationPixels.forEach {
                    canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 12.0f, outlinePaint)
                }
                sensorsDrawn = true
            }
        } ?: Timber.e("Canvas is null!")
    }

    fun setLocations(userLocation: Location, sensorLocations: List<Location>) {
        viewWidth?.let { safeWidth ->

            userLocationPixels = MercatorProjector.getPixelWithScaleFactor(userLocation, scale, safeWidth)

            userLocationPixels?.let { safeUserLocation ->

                val normalized = {
                    val widthCenter = viewWidth!! / 2.0
                    val heightCenter = viewHeight!! / 2.0
                    val diffX = safeUserLocation.x - widthCenter
                    val diffY = safeUserLocation.y - heightCenter
                    Pair(diffX, diffY)
                }()

                safeUserLocation.x -= normalized.first
                safeUserLocation.y -= normalized.second

                sensorLocationPixels = sensorLocations.map {
                    val point = MercatorProjector.getPixelWithScaleFactor(it, scale, safeWidth)
                    point.x -= normalized.first
                    point.y -= normalized.second
                    point
                }
                invalidate()

            } ?: Timber.e("user location was changed to null")
        }
    }

    fun clearLocations() {
        userLocationPixels = null
        sensorsDrawn = false
        invalidate()
    }

}