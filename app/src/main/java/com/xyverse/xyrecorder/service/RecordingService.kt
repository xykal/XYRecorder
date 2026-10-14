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

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        var instance: RecordingService? = null
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private var isRecording = false
    private var isPaused = false
    private var startTime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Log.d(TAG, "RecordingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                Log.d(TAG, "Starting recording with resultCode=$resultCode, data=$data")

                if (resultCode != -1 && data != null) {
                    startForegroundRecording(resultCode, data)
                } else {
                    Log.e(TAG, "Invalid resultCode or data")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                isPaused = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                }
                updateNotification("⏸️ Recording paused", false)
            }
            ACTION_RESUME -> {
                isPaused = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                }
                updateNotification("🎬 Recording...", true)
            }
        }
        return START_STICKY
    }

    private fun startForegroundRecording(resultCode: Int, data: Intent) {
        try {
            // Get MediaProjection
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection")
                stopSelf()
                return
            }

            Log.d(TAG, "MediaProjection obtained")

            // Start foreground service
            startForeground(NOTIFICATION_ID, buildNotification("🎬 Recording started...", true))

            // Setup MediaRecorder
            setupMediaRecorder()
            
            // Create VirtualDisplay
            setupVirtualDisplay()
            
            // Start recording
            mediaRecorder?.start()
            
            isRecording = true
            isPaused = false
            startTime = System.currentTimeMillis()
            
            Log.d(TAG, "Recording started successfully")

            // Start floating overlay
            try {
                val overlayIntent = Intent(this, FloatingOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
                Log.d(TAG, "Floating overlay started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start floating overlay", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopSelf()
        }
    }

    private fun setupMediaRecorder() {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "XYRecorder")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDir, "XYRec_$timestamp.mp4")
        
        Log.d(TAG, "Output file: ${outputFile.absolutePath}")

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
            setVideoEncodingProfileLevel(
                android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel52
            )
            setVideoSize(1280, 720) // 720p
            setVideoFrameRate(30)
            setVideoEncodingBitRate(5_000_000) // 5 Mbps
            setAudioSamplingRate(48000)
            setAudioEncodingBitRate(192_000)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
    }

    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val density = metrics.densityDpi
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "XYRecorder",
            1280,
            720,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
        
        Log.d(TAG, "VirtualDisplay created: ${virtualDisplay?.display}")
    }

    private fun stopRecording() {
        Log.d(TAG, "Stopping recording")
        
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
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping overlay", e)
            }
            
            Log.d(TAG, "Recording stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopRecording", e)
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
        stopRecording()
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
