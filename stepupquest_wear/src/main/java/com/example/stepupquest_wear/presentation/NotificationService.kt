package com.example.stepupquest_wear.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.vector.path
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.example.stepupquest_wear.R // Your R file
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class NotificationService : WearableListenerService() {

    private val NOTIFICATION_CHANNEL_ID = "phone_to_wear_updates"
    private val NOTIFICATION_ID = 101

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/phone_to_wear_updates") {
            val message = String(messageEvent.data)
            // Assuming the message format is "percentage;notificationText"
            val parts = message.split(";")
            if (parts.size == 2) {
                val percentage = parts[0].toIntOrNull()
                val notificationText = parts[1]
                if (percentage != null) {
                    showMilestoneNotification(percentage, notificationText)
                }
            }
        }
    }

    private fun showMilestoneNotification(percentage: Int, text: String) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.splash_icon) // Replace with your notification icon
            .setContentTitle("Step Milestone Reached!")
            .setContentText("$text ($percentage%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For heads-up notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification on tap
            .setOnlyAlertOnce(true) // Alert only the first time for a given notification ID
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen


        // `this@NotificationService` refers to the NotificationService instance, which is a Context
        val notificationManagerCompat = NotificationManagerCompat.from(this@NotificationService)

        // Ensure you have the POST_NOTIFICATIONS permission before calling notify
        // (Handled by the manifest for Wear OS)
        if (ActivityCompat.checkSelfPermission(
                /* context = */ this@NotificationService, // Use the correct Context here
                /* permission = */ Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // It's important to handle the case where permission is not granted.
            // For now, we'll just return, but in a real app, you might want to log this
            // or inform the user.
            return
        }
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name) // Add to strings.xml
            val descriptionText = getString(R.string.notification_channel_description) // Add to strings.xml
            val importance = NotificationManager.IMPORTANCE_HIGH // For heads-up notification
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}