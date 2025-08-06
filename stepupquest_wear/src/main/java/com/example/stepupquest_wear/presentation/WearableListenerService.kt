package com.example.stepupquest_wear.presentation

import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.key.type

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import android.util.Log

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.example.stepupquest_wear.presentation.utils.sendWearNotification

class PhoneUpdatesListenerService : WearableListenerService() {
    companion object {
        const val PHONE_UPDATES_PATH = "/phone_to_wear_updates" // DEBE COINCIDIR
        const val KEY_PERCENTAGE_ACHIEVED = "percentage" // DEBE COINCIDIR
        // ... otras claves que el móvil pueda enviar ...
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == PHONE_UPDATES_PATH) {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    if (dataMap.containsKey(KEY_PERCENTAGE_ACHIEVED)) {
                        val percentage = dataMap.getInt(KEY_PERCENTAGE_ACHIEVED)
                        var title = "Progreso de Meta"
                        var message = ""

                        if (percentage >= 80) {
                            message = "¡Llegaste al 80% de tu meta diaria!"
                            title = "¡Casi allí!"
                        } else if (percentage >= 50) {
                            message = "¡Has alcanzado el 50% de tu meta!"
                            title = "¡Buen trabajo!"
                        }

                        if (message.isNotEmpty()) {
                            sendWearNotification(
                                context = applicationContext,
                                notificationId = percentage, // Evita duplicados si el móvil reenvía
                                title = title,
                                content = message,
                                channelId = "goal_alerts_channel_wear",
                                channelName = "Alertas de Meta Wear"
                            )
                        }
                    }
                    // Aquí el wearable también podría leer "current_phone_steps" si lo necesita
                }
            }
        }
        dataEvents.release()
    }
}