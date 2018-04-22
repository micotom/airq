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


class SensorLocationMap @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

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

    private var userLocation: Location? = null
    private var sensorLocations: List<Location> = emptyList()

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
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
        viewWidth = w
    }

    override fun onDraw(canvas: Canvas?) {
        when (canvas != null && viewHeight != null && viewWidth != null && userLocation != null) {
            true -> {
                val scale = 5000.0
                val userPoint = MercatorProjector.getPixelWithScaleFactor(userLocation!!, scale, viewWidth!!)
                val normalized = {
                    val widthCenter = viewWidth!! / 2.0
                    val heightCenter = viewHeight!! / 2.0
                    val diffX = userPoint.x - widthCenter
                    val diffY = userPoint.y - heightCenter
                    Pair(diffX, diffY)
                }()
                userPoint.x -= normalized.first
                userPoint.y -= normalized.second

                canvas!!.drawCircle(userPoint.x.toFloat(), userPoint.y.toFloat(), currentPointRadius + 2.0f, outlinePaint)
                canvas.drawCircle(userPoint.x.toFloat(), userPoint.y.toFloat(), currentPointRadius, secPaint)
                sensorLocations.forEach {
                    val point = MercatorProjector.getPixelWithScaleFactor(it, scale, viewWidth!!)
                    point.x -= normalized.first
                    point.y -= normalized.second
                    canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 12.0f,
                            outlinePaint)
                }
                sensorsDrawn = true
            }
        }
    }

    fun setLocations(userLocation: Location, sensorLocations: List<Location>) {
        this.userLocation = userLocation
        this.sensorLocations = sensorLocations
        invalidate()
    }

}