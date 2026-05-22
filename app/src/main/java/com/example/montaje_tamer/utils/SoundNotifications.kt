package com.example.montaje_tamer.utils

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object SoundNotifications {
    fun playNotificationSound(context: Context) {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
            Log.d("SoundNotifications", "Sonido de notificación reproducido")
        } catch (e: Exception) {
            Log.e("SoundNotifications", "Error al reproducir sonido", e)
        }
    }
}
