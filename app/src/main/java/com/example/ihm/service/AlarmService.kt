package com.example.ihm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.ihm.MainActivity
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmId: Int = -1

    companion object {
        const val ACTION_STOP = "STOP_ALARM"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val idToMark = intent.getIntExtra("ALARM_ID", -1)
            if (idToMark != -1) {
                markAlarmAsDone(idToMark)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra("ALARM_TITLE") ?: "Alarma activada"
        alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1

        createNotificationChannel()
        
        val stopIntent = Intent(this, AlarmService::class.java).apply { 
            action = ACTION_STOP 
            putExtra("ALARM_ID", alarmId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, if (alarmId != -1) alarmId else 1001, stopIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, if (alarmId != -1) alarmId else 1001, fullScreenIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
            .setContentTitle("Alarma: $title")
            .setContentText("La alarma está sonando")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(1001, notification)
        startAlarm()

        return START_STICKY
    }

    private fun markAlarmAsDone(id: Int) {
        val database = AppDatabase.getDatabase(this)
        val repository = AgendaRepository(database.recordatorioDao())
        CoroutineScope(Dispatchers.IO).launch {
            repository.markAsCompletado(id)
        }
    }

    private fun startAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                isLooping = true
                prepare()
                start()
            }

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 1000, 1000)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ALARM_CHANNEL",
            "Alarmas activas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para alarmas sonoras"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
