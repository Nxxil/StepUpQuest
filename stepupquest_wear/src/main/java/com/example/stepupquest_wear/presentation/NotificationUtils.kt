package com.example.stepupquest_wear.presentation.utils // O el paquete que hayas elegido

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stepupquest_wear.R // Necesitarás un ícono de notificación
import com.example.stepupquest_wear.presentation.MainActivity // Para abrir la app al tocar

/**
 * Envía una notificación local en el dispositivo Wear OS.
 *
 * @param context Contexto de la aplicación.
 * @param notificationId ID único para la notificación (útil para actualizar o cancelar).
 * @param title Título de la notificación.
 * @param content Contenido/texto principal de la notificación.
 * @param channelId ID del canal de notificación (importante para Android Oreo y superior).
 * @param channelName Nombre visible del canal de notificación.
 * @param channelDescription Descripción del canal de notificación.
 * @param priority Prioridad de la notificación (afecta cómo se muestra).
 */
fun sendWearNotification(
    context: Context,
    notificationId: Int,
    title: String,
    content: String,
    channelId: String,
    channelName: String,
    channelDescription: String = "Notificaciones generales de la aplicación",
    priority: Int = NotificationCompat.PRIORITY_HIGH
) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Crear canal de notificación para Android Oreo (API 26) y superior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // Usar HIGH para que aparezca como Heads-Up
            ).apply {
                description = channelDescription
                // Opcional: Configurar luces, vibración, etc.
                // enableLights(true)
                // lightColor = Color.RED
                // enableVibration(true) // Ya debería vibrar por defecto con IMPORTANCE_HIGH
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationUtils", "Canal de notificación '$channelName' creado.")
        }
    }

    // Intent para abrir la MainActivity cuando se toca la notificación
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        notificationId, // Usa notificationId como requestCode para que sea único si creas varios PendingIntents
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notificacion) // **REEMPLAZA ESTO**
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(priority)
        .setContentIntent(pendingIntent) // Acción al tocar la notificación
        .setAutoCancel(true) // La notificación se cierra al tocarla
        .setCategory(NotificationCompat.CATEGORY_EVENT) // Categoría apropiada
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible en pantalla de bloqueo

    // En Wear OS, las notificaciones de apps emparejadas se puentean por defecto.
    // Si quieres asegurar que esta notificación es *local* y no un puente,
    // puedes considerar .setLocalOnly(true), pero para notificaciones generadas
    // directamente en el wearable, esto no suele ser necesario.

    try {
        notificationManager.notify(notificationId, builder.build())
        Log.d("NotificationUtils", "Notificación '$title' enviada con ID $notificationId")
    } catch (e: SecurityException) {
        Log.e(
            "NotificationUtils",
            "Error de seguridad al enviar notificación. ¿Falta permiso POST_NOTIFICATIONS en Manifest?",
            e
        )
    } catch (e: Exception) {
        Log.e("NotificationUtils", "Error general al enviar notificación", e)
    }
}