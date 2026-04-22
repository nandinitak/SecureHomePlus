package com.example.securehomeplus.view.weather

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class WeatherCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    private var temperature = 0f
    private var targetTemperature = 0f
    private var humidity = 0f
    private var targetHumidity = 0f
    
    private val gradientColors = intArrayOf(
        Color.parseColor("#4DD0E1"),
        Color.parseColor("#26C6DA"),
        Color.parseColor("#00ACC1")
    )

    init {
        paint.style = Paint.Style.FILL
    }

    fun setWeatherData(temp: Float, humid: Float) {
        targetTemperature = temp
        targetHumidity = humid
        animateValues()
    }

    private fun animateValues() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                temperature = targetTemperature * fraction
                humidity = targetHumidity * fraction
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw gradient background
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            gradientColors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        // Draw animated wave pattern
        drawWavePattern(canvas)
        
        // Draw temperature arc
        drawTemperatureArc(canvas)
    }

    private fun drawWavePattern(canvas: Canvas) {
        paint.color = Color.parseColor("#80FFFFFF")
        paint.style = Paint.Style.FILL
        
        path.reset()
        path.moveTo(0f, height * 0.7f)
        
        val waveLength = width / 2f
        val amplitude = 30f
        val offset = (System.currentTimeMillis() % 3000) / 3000f * waveLength
        
        var x = -offset
        while (x < width + waveLength) {
            val y = height * 0.7f + amplitude * sin((x / waveLength) * 2 * Math.PI).toFloat()
            path.lineTo(x, y)
            x += 10f
        }
        
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        
        canvas.drawPath(path, paint)
        
        // Animate wave
        postInvalidateDelayed(50)
    }

    private fun drawTemperatureArc(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width / 3f
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 20f
        paint.color = Color.parseColor("#40FFFFFF")
        
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        canvas.drawArc(rect, 135f, 270f, false, paint)
        
        // Draw temperature progress
        paint.color = Color.WHITE
        val sweepAngle = (temperature / 50f) * 270f
        canvas.drawArc(rect, 135f, sweepAngle, false, paint)
        
        paint.style = Paint.Style.FILL
    }
}
