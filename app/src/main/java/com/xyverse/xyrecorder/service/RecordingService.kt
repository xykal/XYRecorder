package com.xyverse.xyrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.xyverse.xyrecorder.MainActivity
import com.xyverse.xyrecorder.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "xyrecorder_recording"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "RecordingService"

        const val ACTION_START = "com.xyverse.xyrecorder.ACTION_START"
        const val ACTION_STOP = "com.xyverse.xyrecorder.ACTION_STOP"
        const val ACTION_PAUSE = "com.xyverse.xyrecorder.ACTION_PAUSE"
        const val ACTION_RESUME = "com.xyverse.xyrecorder.ACTION_RESUME"

        // Static storage for MediaProjection data
        private var pendingResultCode: Int = -1
        private var pendingData: Intent? = null
        
        fun setRecordingData(resultCode: Int, data: Intent?) {
            Log.d(TAG, "📦 Storing recording data: resultCode=$resultCode, data=${data != null}")
            pendingResultCode = resultCode
            pendingData = data
        }

        var instance: RecordingService? = null
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private var isRecording = false
    private var isPaused = false
    private var startTime = 0L
    private var outputFile: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Log.d(TAG, "✅ RecordingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📱 onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "🎬 Starting recording with stored data...")
                
                if (pendingResultCode != -1 && pendingData != null) {
                    startRecordingInternal(pendingResultCode, pendingData!!)
                } else {
                    Log.e(TAG, "❌ No recording data available!")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "⏹ Stopping recording")
                stopRecordingInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                Log.d(TAG, "⏸ Pausing recording")
                isPaused = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                }
                updateNotification("⏸️ Recording paused", false)
            }
            ACTION_RESUME -> {
                Log.d(TAG, "▶ Resuming recording")
                isPaused = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                }
                updateNotification("🎬 Recording...", true)
            }
        }
        return START_STICKY
    }

    private fun startRecordingInternal(resultCode: Int, data: Intent) {
        try {
            Log.d(TAG, "🔧 Setting up MediaProjection...")
            
            // Step 1: Get MediaProjection
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            if (mediaProjection == null) {
                Log.e(TAG, "❌ MediaProjection is null!")
                stopSelf()
                return
            }

            Log.d(TAG, "✅ MediaProjection obtained")

            // Step 2: Start foreground FIRST
            Log.d(TAG, "🔧 Starting foreground service...")
            startForeground(NOTIFICATION_ID, buildNotification("🎬 Recording started...", true))

            // Step 3: Create output file
            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "XYRecorder")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
                Log.d(TAG, "📁 Created directory: ${outputDir.absolutePath}")
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outputDir, "XYRec_$timestamp.mp4")
            outputFile = file.absolutePath
            Log.d(TAG, "📁 Output file: ${file.absolutePath}")

            // Step 4: Setup MediaRecorder
            Log.d(TAG, "🔧 Setting up MediaRecorder...")
            setupMediaRecorder(file.absolutePath)
            
            // Step 5: Create VirtualDisplay
            Log.d(TAG, "🔧 Setting up VirtualDisplay...")
            setupVirtualDisplay()
            
            // Step 6: Start recording
            Log.d(TAG, "▶ Starting MediaRecorder...")
            mediaRecorder?.start()
            
            isRecording = true
            isPaused = false
            startTime = System.currentTimeMillis()
            
            Log.d(TAG, "✅ Recording started successfully!")

            // Clear pending data
            pendingResultCode = -1
            pendingData = null

            // Step 7: Start floating overlay
            Log.d(TAG, "🫧 Starting floating overlay...")
            try {
                val overlayIntent = Intent(this, FloatingOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
                Log.d(TAG, "✅ Floating overlay started")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to start floating overlay", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting recording", e)
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupMediaRecorder(outputPath: String) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1280, 720)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(5_000_000)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(outputPath)
            
            Log.d(TAG, "🔧 Preparing MediaRecorder...")
            prepare()
            Log.d(TAG, "✅ MediaRecorder prepared")
        }
    }

    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val density = metrics.densityDpi
        
        val surface = mediaRecorder?.surface
        if (surface == null) {
            Log.e(TAG, "❌ MediaRecorder surface is null!")
            return
        }
        
        Log.d(TAG, "🔧 Creating VirtualDisplay: 1280x720, density=$density")
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "XYRecorder",
            1280,
            720,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
        
        if (virtualDisplay == null) {
            Log.e(TAG, "❌ VirtualDisplay is null!")
        } else {
            Log.d(TAG, "✅ VirtualDisplay created: ${virtualDisplay?.display}")
        }
    }

    private fun stopRecordingInternal() {
        Log.d(TAG, "🔧 Stopping recording...")
        
        try {
            isRecording = false
            isPaused = false
            
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            
            virtualDisplay?.release()
            virtualDisplay = null
            
            mediaProjection?.stop()
            mediaProjection = null
            
            // Stop overlay
            try {
                stopService(Intent(this, FloatingOverlayService::class.java))
                Log.d(TAG, "✅ Overlay stopped")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error stopping overlay", e)
            }
            
            Log.d(TAG, "✅ Recording stopped: $outputFile")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in stopRecording", e)
            e.printStackTrace()
        }
    }

    fun getStatus(): Triple<Boolean, Boolean, Long> {
        val duration = if (isRecording && !isPaused) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
        return Triple(isRecording, isPaused, duration)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "🗑️ RecordingService destroyed")
        if (isRecording) {
            stopRecordingInternal()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "✅ Notification channel created")
        }
    }

    private fun buildNotification(text: String, isRecording: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }.let { intent ->
            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val pauseIntent = Intent(this, RecordingService::class.java).apply {
            action = if (isRecording) ACTION_PAUSE else ACTION_RESUME
        }.let { intent ->
            PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎬 XYRecorder")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_launcher_foreground, if (isRecording) "⏸ Pause" else "▶ Resume", pauseIntent)
            .addAction(R.drawable.ic_launcher_foreground, "⏹ Stop", stopIntent)
            .build()
    }

    private fun updateNotification(text: String, isRecording: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text, isRecording))
    }
}
