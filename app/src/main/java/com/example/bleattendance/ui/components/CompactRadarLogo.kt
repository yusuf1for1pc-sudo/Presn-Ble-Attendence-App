package com.example.bleattendance.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.*

class CompactRadarLogo @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Colors matching your app theme
    private val backgroundColor = Color.parseColor("#3a3a3a")
    private val radarColor = Color.parseColor("#4ade80")  // Green from your design
    private val accentColor = Color.parseColor("#6366f1")  // Purple accent
    private val studentColorRed = Color.parseColor("#ef4444")  // Red dot color
    private val studentColorGreen = Color.parseColor("#4ade80")  // Green dot color
    private val confirmColor = Color.parseColor("#10b981")  // Confirmation color
    
    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.FILL
    }
    
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    
    private val studentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = studentColorRed
        style = Paint.Style.FILL
    }
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    // Dimensions
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    
    // Animation properties
    private val radarAlphas = floatArrayOf(0.3f, 0.5f)
    private val radarScales = floatArrayOf(1f, 1f)
    private var centerPulse = 1f
    private var sweepAngle = 0f
    
    // Student animation properties
    data class Student(
        var x: Float = 0f,
        var y: Float = 0f,
        var alpha: Float = 0f,
        var scale: Float = 0f,
        var lineAlpha: Float = 0f,
        var isFadingOut: Boolean = false,
        var isGreen: Boolean = false
    )
    
    private val students = mutableListOf<Student>()
    private var isAnimating = false
    
    private val animators = mutableListOf<ValueAnimator>()
    
    init {
        startAnimation()
        startStudentAnimation()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) / 2f * 0.7f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw rounded background
        val backgroundRadius = minOf(width, height) / 2f * 0.9f
        canvas.drawRoundRect(
            centerX - backgroundRadius, centerY - backgroundRadius,
            centerX + backgroundRadius, centerY + backgroundRadius,
            backgroundRadius * 0.3f, backgroundRadius * 0.3f,
            backgroundPaint
        )
        
        // Draw radar circles
        drawRadarCircles(canvas)
        
        // Draw radar sweep
        drawRadarSweep(canvas)
        
        // Draw center dot
        val centerRadius = 6f * centerPulse
        canvas.drawCircle(centerX, centerY, centerRadius, centerDotPaint)
        
        // Draw students
        drawStudents(canvas)
    }
    
    private fun drawStudents(canvas: Canvas) {
        students.forEach { student ->
            // Draw connection line
            if (student.lineAlpha > 0) {
                canvas.save()
                linePaint.alpha = (student.lineAlpha * 255).toInt()
                canvas.drawLine(centerX, centerY, student.x, student.y, linePaint)
                canvas.restore()
            }
            
            // Draw student dot
            if (student.alpha > 0) {
                canvas.save()
                studentPaint.alpha = (student.alpha * 255).toInt()
                studentPaint.color = when {
                    student.isGreen -> studentColorGreen
                    else -> studentColorRed
                }
                canvas.scale(student.scale, student.scale, student.x, student.y)
                canvas.drawCircle(student.x, student.y, 8f, studentPaint)
                canvas.restore()
            }
        }
    }
    
    private fun drawRadarCircles(canvas: Canvas) {
        val radii = floatArrayOf(radius * 0.4f, radius * 0.8f)
        
        radii.forEachIndexed { index, r ->
            radarPaint.alpha = (radarAlphas[index] * 255).toInt()
            canvas.save()
            canvas.scale(radarScales[index], radarScales[index], centerX, centerY)
            canvas.drawCircle(centerX, centerY, r, radarPaint)
            canvas.restore()
        }
    }
    
    private fun drawRadarSweep(canvas: Canvas) {
        // Draw radar sweep line
        val sweepX = centerX + cos(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius * 0.8f
        val sweepY = centerY + sin(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius * 0.8f
        
        radarPaint.alpha = 180
        radarPaint.strokeWidth = 2f
        canvas.drawLine(centerX, centerY, sweepX, sweepY, radarPaint)
        
        // Draw fade gradient behind sweep
        val gradient = RadialGradient(
            centerX, centerY, radius * 0.8f,
            intArrayOf(
                Color.argb(30, 67, 222, 128),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        
        canvas.save()
        canvas.rotate(sweepAngle - 30f, centerX, centerY)
        val rect = RectF(centerX - radius * 0.8f, centerY - radius * 0.8f, 
                        centerX + radius * 0.8f, centerY + radius * 0.8f)
        canvas.drawArc(rect, -15f, 30f, true, gradientPaint)
        canvas.restore()
    }
    
    private fun startStudentAnimation() {
        // Create periodic student animations
        val studentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6000
            repeatCount = ValueAnimator.INFINITE
            startDelay = 2000
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                if (progress < 0.1f && !isAnimating) {
                    // Create new student
                    val angle = (Math.random() * 2 * PI).toFloat()
                    val distance = radius * 0.6f + (Math.random() * radius * 0.2f).toFloat()
                    val student = Student(
                        x = centerX + cos(angle.toDouble()).toFloat() * distance,
                        y = centerY + sin(angle.toDouble()).toFloat() * distance
                    )
                    students.add(student)
                    animateStudent(student)
                }
            }
        }
        animators.add(studentAnimator)
        studentAnimator.start()
    }
    
    private fun animateStudent(student: Student) {
        isAnimating = true
        
        // Phase 1: Red dot appears
        val appearAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.alpha = progress
                student.isGreen = false // Start as red
                invalidate()
            }
        }
        
        val scaleUpAnimator = ValueAnimator.ofFloat(0f, 1.3f).apply {
            duration = 250
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.scale = progress
                invalidate()
            }
        }
        
        val scaleDownAnimator = ValueAnimator.ofFloat(1.3f, 1f).apply {
            duration = 250
            startDelay = 250
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.scale = progress
                invalidate()
            }
        }
        
        // Phase 2: Connection line appears
        val lineAppearAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            startDelay = 1000
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.lineAlpha = progress
                invalidate()
            }
        }
        
        // Phase 3: Turn green and fade out
        val turnGreenAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            startDelay = 1500
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.isGreen = true // Turn green
                invalidate()
            }
        }
        
        val lineFadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            startDelay = 1500
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.lineAlpha = progress
                invalidate()
            }
        }
        
        // Phase 4: Fade out
        val fadeOutAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 800
            startDelay = 1800
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                student.alpha = progress
                student.scale = 0.3f + (progress * 0.7f)
                student.isFadingOut = true
                invalidate()
            }
        }
        
        // Start animations in sequence
        appearAnimator.start()
        scaleUpAnimator.start()
        scaleDownAnimator.start()
        lineAppearAnimator.start()
        turnGreenAnimator.start()
        lineFadeAnimator.start()
        fadeOutAnimator.start()
        
        // Clean up student after animation
        fadeOutAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (animation.animatedFraction >= 1f) {
                    students.remove(student)
                    isAnimating = false
                    invalidate()
                }
            }
        })
    }
    
    fun startAnimation() {
        stopAnimation() // Clear any existing animations
        
        // Radar pulse animation
        val radarAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                radarAlphas[0] = 0.3f + 0.4f * sin(progress * 2 * PI).toFloat()
                radarAlphas[1] = 0.5f + 0.3f * sin(progress * 2 * PI + PI/2).toFloat()
                radarScales[0] = 1f + 0.05f * sin(progress * 2 * PI).toFloat()
                radarScales[1] = 1f + 0.03f * sin(progress * 2 * PI + PI/4).toFloat()
                invalidate()
            }
        }
        
        // Center dot pulse
        val centerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                centerPulse = 1f + 0.3f * sin(progress * 2 * PI).toFloat()
                invalidate()
            }
        }
        
        // Radar sweep
        val sweepAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                sweepAngle = animation.animatedValue as Float
                invalidate()
            }
        }
        
        animators.addAll(listOf(radarAnimator, centerAnimator, sweepAnimator))
        animators.forEach { it.start() }
    }
    
    fun stopAnimation() {
        animators.forEach { 
            it.cancel() 
        }
        animators.clear()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
