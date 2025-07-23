package com.utnay.stepupquest

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerService : WearableListenerService() {

    companion object {
        const val STEP_DATA_PATH = "/step-data"
        const val TAG = "DataLayerService"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == STEP_DATA_PATH) {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val stepsFromWear = dataMap.getInt("steps", 0)
                    val timestamp = dataMap.getLong("timestamp", System.currentTimeMillis())

                    // Actualizar los pasos totales
                    updateTotalSteps(stepsFromWear)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/step-update" -> {
                val stepsData = String(messageEvent.data)
                Log.d(TAG, "Received step update: $stepsData")
                // Procesar datos de pasos del wearable
            }
        }
    }

    private fun updateTotalSteps(wearSteps: Int) {
        // Esta función será llamada cuando se reciban datos del wearable
        // Deberías implementar la lógica para combinar los pasos del móvil y el wearable
        Log.d(TAG, "Steps from wear: $wearSteps")
    }
}