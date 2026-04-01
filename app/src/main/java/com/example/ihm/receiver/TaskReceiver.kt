package com.example.ihm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ihm.MainActivity
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_MARK_DONE = "ACTION_MARK_DONE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TYPE = "EXTRA_TYPE" // "TASK" o "EVENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra("TITLE") ?: "Recordatorio"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "TASK"

        if (intent.action == ACTION_MARK_DONE) {
            markAsDone(context, taskId)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(taskId)
            return
        }

        showNotification(context, taskId, title, type)
    }

    private fun showNotification(context: Context, id: Int, title: String, type: String) {
        val channelId = "TASK_EVENT_CHANNEL"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tareas y Eventos", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, id, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (type == "EVENT") "Próximo Evento" else "Tarea pendiente")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        // Solo tareas tienen el "checkbox" (botón de acción en la notificación)
        if (type == "TASK") {
            val doneIntent = Intent(context, TaskReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra(EXTRA_TASK_ID, id)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context, id + 10000, doneIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.checkbox_on_background, "Marcar como hecho", donePendingIntent)
        }

        notificationManager.notify(id, builder.build())
    }

    private fun markAsDone(context: Context, id: Int) {
        if (id == -1) return
        val database = AppDatabase.getDatabase(context)
        val repository = AgendaRepository(database.recordatorioDao())
        CoroutineScope(Dispatchers.IO).launch {
            repository.markAsCompletado(id)
        }
    }
}
