package com.xyverse.xyrecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xyverse.xyrecorder.R
import com.xyverse.xyrecorder.audio.AudioRecorder
import java.io.File

/**
 * Foreground service for audio recording
 * Keeps recording even when app is in background
 */
class AudioRecordingService : Service() {
    
    companion object {
        private const val TAG = "AudioRecordingService"
        private const val CHANNEL_ID = "xyrecorder_recording"
        private const val NOTIFICATION_ID = 1
        
        const val ACTION_START = "com.xyverse.xyrecorder.START_RECORDING"
        const val ACTION_STOP = "com.xyverse.xyrecorder.STOP_RECORDING"
    }
    
    private val binder = LocalBinder()
    private var audioRecorder: AudioRecorder? = null
    
    var onAudioLevelUpdate: ((Float) -> Unit)? = null
    var onRecordingComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize audio recorder
        audioRecorder = AudioRecorder(this).apply {
            onAudioLevelUpdate = { level ->
                this@AudioRecordingService.onAudioLevelUpdate?.invoke(level)
                updateNotification(level)
            }
            
            onRecordingComplete = { file ->
                this@AudioRecordingService.onRecordingComplete?.invoke(file)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            
            onError = { error ->
                this@AudioRecordingService.onError?.invoke(error)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Starting recording")
                startForeground(NOTIFICATION_ID, createNotification("Recording..."))
                audioRecorder?.startRecording()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping recording")
                audioRecorder?.stopRecording()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
        audioRecorder = null
        Log.d(TAG, "Service destroyed")
    }
    
    fun startRecording() {
        audioRecorder?.startRecording()
    }
    
    fun stopRecording() {
        audioRecorder?.stopRecording()
    }
    
    fun isRecording(): Boolean = audioRecorder?.isRecording() ?: false
    
    fun updateNoiseSettings(enabled: Boolean) {
        audioRecorder?.updateNoiseSettings(enabled)
    }
    
    fun updatePreset(preset: String) {
        audioRecorder?.updatePreset(preset)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when XYRecorder is recording audio"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XYRecorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(levelDb: Float) {
        val text = "Recording... ${String.format("%.1f", levelDb)} dB"
        val notification = createNotification(text)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
