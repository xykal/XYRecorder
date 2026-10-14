package com.xyverse.xyrecorder.service

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import com.xyverse.xyrecorder.R
import java.util.*

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        var instance: FloatingOverlayService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
    }

    private fun createFloatingView() {
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        floatingView = FrameLayout(this).apply {
            layoutParams = params
            
            // Create circular bubble with gradient
            val bubbleSize = 140
            val bubble = object : View(this@FloatingOverlayService) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                private val gradient = RadialGradient(
                    bubbleSize / 2f, bubbleSize / 2f, bubbleSize / 2f,
                    Color.parseColor("#FF6C3FFF"),
                    Color.parseColor("#FF00D4AA"),
                    Shader.TileMode.CLAMP
                )

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    paint.shader = gradient
                    canvas.drawCircle(bubbleSize / 2f, bubbleSize / 2f, bubbleSize / 2f - 4f, paint)
                    
                    // Draw recording indicator
                    paint.shader = null
                    paint.color = Color.WHITE
                    canvas.drawCircle(bubbleSize / 2f, bubbleSize / 2f, 20f, paint)
                }
            }

            bubble.layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            addView(bubble)

            // Timer text
            val timerText = TextView(this@FloatingOverlayService).apply {
                text = "00:00"
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#CC000000"))
                setPadding(12, 6, 12, 6)
            }

            val timerParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = -30
            }
            timerText.layoutParams = timerParams
            addView(timerText)

            // Update timer
            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    RecordingService.instance?.let { service ->
                        val (_, _, duration) = service.getStatus()
                        val seconds = (duration / 1000).toInt()
                        val mins = seconds / 60
                        val secs = seconds % 60
                        post {
                            timerText.text = String.format("%02d:%02d", mins, secs)
                        }
                    }
                }
            }, 0, 1000)

            // Click to stop
            bubble.setOnClickListener {
                val stopIntent = Intent(this@FloatingOverlayService, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                startService(stopIntent)
            }

            // Drag to move
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            bubble.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        true
                    }
                    else -> false
                }
            }
        }

        windowManager?.addView(floatingView, params)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
