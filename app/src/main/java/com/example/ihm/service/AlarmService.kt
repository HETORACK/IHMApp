package com.example.ihm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
        const val CHANNEL_ID = "ALARM_SOUND_CHANNEL"
        const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val idToMark = intent.getIntExtra("ALARM_ID", -1)
            if (idToMark != -1) markAlarmAsDone(idToMark)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra("ALARM_TITLE") ?: "Alarma activada"
        alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1

        createNotificationChannel()
        
        // Intent para detener desde el botón de la notificación
        val stopIntent = Intent(this, AlarmService::class.java).apply { 
            action = ACTION_STOP 
            putExtra("ALARM_ID", alarmId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, alarmId, stopIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent para abrir la app al tocar la notificación
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId, fullScreenIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("¡Alarma: $title!")
            .setContentText("Pulsa para abrir o detener para silenciar")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Aparece sobre la pantalla de bloqueo
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DETENER", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
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
            stopAlarm() // Limpiar si había algo sonando

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                isLooping = true
                // CRUCIAL: Mantiene la CPU despierta mientras suena la música
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK) 
                prepare()
                start()
            }

            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 500, 500)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmas Críticas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alarmas que requieren atención inmediata"
                setSound(null, null) // El sonido lo manejamos con MediaPlayer para control total
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
