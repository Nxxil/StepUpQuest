package com.utnay.stepupquest

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StepDataManager(private val context: Context) {

    companion object {
        const val STEP_DATA_PATH = "/step-data"
        const val TAG = "StepDataManager"
    }

    fun sendStepsToWear(steps: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val putDataMapRequest = PutDataMapRequest.create(STEP_DATA_PATH)
                putDataMapRequest.dataMap.putInt("steps", steps)
                putDataMapRequest.dataMap.putLong("timestamp", System.currentTimeMillis())

                val putDataRequest = putDataMapRequest.asPutDataRequest()
                putDataRequest.setUrgent()

                val wearClient = Wearable.getDataClient(context)
                val result = wearClient.putDataItem(putDataRequest).await()

                Log.d(TAG, "Data sent to wear: $steps, result: ${result.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending data to wear: ${e.message}")
            }
        }
    }

    fun sendNotificationToWear(percentage: Int, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationPath = "/phone_to_wear_updates"
                val putDataMapRequest = PutDataMapRequest.create(notificationPath)
                putDataMapRequest.dataMap.putInt("percentage_achieved", percentage)
                putDataMapRequest.dataMap.putString("message", message)
                putDataMapRequest.dataMap.putLong("timestamp", System.currentTimeMillis())

                val putDataRequest = putDataMapRequest.asPutDataRequest()
                putDataRequest.setUrgent()

                val wearClient = Wearable.getDataClient(context)
                val result = wearClient.putDataItem(putDataRequest).await()

                Log.d(TAG, "Notification sent to wear: $message, result: ${result.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification to wear: ${e.message}")
            }
        }
    }
}