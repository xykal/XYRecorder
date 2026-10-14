package com.xyverse.xyrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xyverse.xyrecorder.MainActivity
import com.xyverse.xyrecorder.R
import com.xyverse.xyrecorder.recorder.ScreenRecorder

/**
 * Foreground service that keeps the recording alive even when the app is backgrounded.
 */
class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "xyrecorder_recording"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.xyverse.xyrecorder.ACTION_START"
        const val ACTION_STOP = "com.xyverse.xyrecorder.ACTION_STOP"
        const val ACTION_PAUSE = "com.xyverse.xyrecorder.ACTION_PAUSE"
        const val ACTION_RESUME = "com.xyverse.xyrecorder.ACTION_RESUME"

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        var instance: RecordingService? = null
            private set
    }

    private lateinit var recorder: ScreenRecorder

    override fun onCreate() {
        super.onCreate()
        instance = this
        recorder = ScreenRecorder(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (data != null) {
                    startForeground(NOTIFICATION_ID, buildNotification("Recording...", isRecording = true))

                    // Recorder settings come from MainActivity via shared preferences / direct call
                    // For simplicity we start here with default settings
                    // In production, pass settings via intent extras or DataStore
                    recorder.startRecording(
                        resultCode = resultCode,
                        data = data,
                        videoSettings = com.xyverse.xyrecorder.data.VideoSettings(),
                        audioSettings = com.xyverse.xyrecorder.data.AudioSettings(),
                        nrSettings = com.xyverse.xyrecorder.data.NoiseReductionSettings()
                    )
                }
            }
            ACTION_STOP -> {
                recorder.stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                recorder.pauseRecording()
                updateNotification("Recording paused", isRecording = false)
            }
            ACTION_RESUME -> {
                recorder.resumeRecording()
                updateNotification("Recording...", isRecording = true)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        recorder.stopRecording()
    }

    fun getRecorder(): ScreenRecorder = recorder

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
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent: PendingIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }.let { intent ->
            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val pauseIntent: PendingIntent = Intent(this, RecordingService::class.java).apply {
            action = if (isRecording) ACTION_PAUSE else ACTION_RESUME
        }.let { intent ->
            PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XYRecorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, if (isRecording) "Pause" else "Resume", pauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)

        return builder.build()
    }

    private fun updateNotification(text: String, isRecording: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text, isRecording))
    }
}
