package com.example.ihm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ihm.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("ALARM_TITLE") ?: "Alarma"
        val id = intent.getIntExtra("ALARM_ID", -1)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_ID", id)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
