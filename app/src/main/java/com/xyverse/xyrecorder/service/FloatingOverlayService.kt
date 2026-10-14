package com.xyverse.xyrecorder.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.xyverse.xyrecorder.R
import com.xyverse.xyrecorder.recorder.ScreenRecorder

/**
 * Floating overlay bubble service - like XRecorder.
 * Shows a floating control bubble that stays on screen even when app is backgrounded.
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var expandedView: LinearLayout? = null
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
        createExpandedView()
    }

    private fun createBubble() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        bubbleView = FrameLayout(this).apply {
            layoutParams = params
            
            // Bubble button
            val bubbleButton = ImageView(this@FloatingOverlayService).apply {
                setImageResource(R.drawable.ic_recording)
                setColorFilter(android.graphics.Color.WHITE)
                setBackgroundResource(R.drawable.bubble_background)
                setPadding(16, 16, 16, 16)
                
                setOnClickListener {
                    toggleExpanded()
                }
                
                var initialX = 0
                var initialY = 0
                var initialTouchX = 0f
                var initialTouchY = 0f
                
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(bubbleView, params)
                            true
                        }
                        else -> false
                    }
                }
            }
            
            addView(bubbleButton)
        }

        windowManager?.addView(bubbleView, params)
    }

    private fun createExpandedView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        expandedView = LinearLayout(this).apply {
            layoutParams = params
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundResource(R.drawable.expanded_background)
            visibility = android.view.View.GONE

            // Title
            addView(TextView(this@FloatingOverlayService).apply {
                text = "XYRecorder"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                gravity = android.view.Gravity.CENTER
            })

            // Status
            addView(TextView(this@FloatingOverlayService).apply {
                id = android.R.id.text1
                text = "Recording: 00:00"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16)
            })

            // Buttons row
            val buttonsRow = LinearLayout(this@FloatingOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
            }

            // Pause/Resume button
            buttonsRow.addView(createActionButton("⏸️") {
                RecordingService.instance?.recorder?.let { recorder ->
                    if (recorder.isPaused) {
                        recorder.resumeRecording()
                    } else {
                        recorder.pauseRecording()
                    }
                }
            })

            // Stop button
            buttonsRow.addView(createActionButton("⏹️") {
                val stopIntent = Intent(this@FloatingOverlayService, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                startService(stopIntent)
            })

            addView(buttonsRow)
        }

        windowManager?.addView(expandedView, params)
    }

    private fun createActionButton(emoji: String, onClick: () -> Unit): android.widget.Button {
        return android.widget.Button(this).apply {
            text = emoji
            textSize = 20f
            setPadding(32, 16, 32, 16)
            setBackgroundResource(R.drawable.button_background)
            setOnClickListener { onClick() }
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            layoutParams = params
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        bubbleView?.visibility = if (isExpanded) android.view.View.GONE else android.view.View.VISIBLE
        expandedView?.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(bubbleView)
        windowManager?.removeView(expandedView)
    }
}
