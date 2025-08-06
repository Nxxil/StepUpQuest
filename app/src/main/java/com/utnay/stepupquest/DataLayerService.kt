package com.utnay.stepupquest

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
// import com.google.android.gms.wearable.DataMap //
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.util.Calendar // Para el reseteo diario

class DataLayerService : WearableListenerService() {

    companion object {
        const val STEP_DATA_PATH = "/step-data" // Path existente
        const val TAG = "DataLayerService"
        const val KEY_WEAR_STEPS = "steps"      // Key existente para los pasos del wearable
        const val KEY_WEAR_TIMESTAMP = "timestamp" // Key existente para el timestamp

        // SharedPreferences para persistir datos
        private const val PREFS_APP_NAME = "StepUpQuestPrefs"
        private const val PREF_LAST_KNOWN_TOTAL_WEAR_STEPS = "lastKnownTotalWearSteps"
        private const val PREF_TOTAL_STEPS_FROM_WEAR_TODAY = "totalStepsFromWearToday"
        private const val PREF_LAST_RESET_DAY_OF_YEAR = "lastResetDayOfYear"

        // Para notificar a la UI sobre la actualización de pasos
        const val ACTION_WEAR_STEPS_UPDATED = "com.utnay.stepupquest.ACTION_WEAR_STEPS_UPDATED"
        const val EXTRA_UPDATED_STEPS_FROM_WEAR_TODAY = "extraUpdatedStepsFromWearToday"
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext.getSharedPreferences(PREFS_APP_NAME, Context.MODE_PRIVATE)
        resetDailyCountersIfNeeded() // Es bueno llamar esto aquí también
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // super.onDataChanged(dataEvents) // No es estrictamente necesario llamar al super si lo manejas todo

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == STEP_DATA_PATH) {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    // Tu código original usa getInt. Si el wearable envía Long, esto debería ser getLong.
                    // Por ahora, respetaremos getInt según tu código original.
                    val stepsFromWear = dataMap.getInt(KEY_WEAR_STEPS, -1)
                    val timestamp = dataMap.getLong(KEY_WEAR_TIMESTAMP, System.currentTimeMillis())

                    if (stepsFromWear != -1) { // Solo procesar si los pasos son válidos
                        Log.d(TAG, "Datos recibidos del Wear: Steps=$stepsFromWear, Timestamp=$timestamp")
                        updateTotalSteps(stepsFromWear)
                    } else {
                        Log.w(TAG, "Clave '$KEY_WEAR_STEPS' no encontrada o inválida en DataMap del wearable.")
                    }
                }
            }
        }
        dataEvents.release() // ¡Importante liberar el buffer!
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/step-update" -> { // Esto es de tu código original
                val stepsData = String(messageEvent.data)
                Log.d(TAG, "Mensaje de actualización de pasos recibido: $stepsData")
                // Si este mensaje también contiene un conteo de pasos que debe ser procesado
                // de forma similar, necesitarías parsearlo y posiblemente llamar a updateTotalSteps
                // o a una función similar. Ejemplo:
                // try {
                // val stepsFromMessage = stepsData.toInt()
                // updateTotalSteps(stepsFromMessage)
                // } catch (e: NumberFormatException) {
                // Log.e(TAG, "Error al parsear pasos del mensaje: $stepsData", e)
                // }
            }
        }
    }

    /**
     * Actualiza y acumula los pasos recibidos del wearable.
     * @param currentTotalStepsFromWear El conteo TOTAL actual de pasos del dispositivo wearable
     *                                   (se asume que es el total desde el último reinicio del wearable).
     */
    private fun updateTotalSteps(currentTotalStepsFromWear: Int) {
        resetDailyCountersIfNeeded() // Asegurar que trabajamos con contadores del día actual

        // Convertimos a Long para consistencia interna, aunque recibamos Int.
        val newTotalDeviceStepsFromWear = currentTotalStepsFromWear.toLong()

        // Obtener el último total conocido de pasos del wearable del día anterior o de la última sincronización.
        val lastKnownTotalWearSteps = sharedPreferences.getLong(PREF_LAST_KNOWN_TOTAL_WEAR_STEPS, 0L)
        // Obtener los pasos que ya hemos acumulado del wearable para el día de HOY.
        var totalStepsFromWearToday = sharedPreferences.getLong(PREF_TOTAL_STEPS_FROM_WEAR_TODAY, 0L)

        Log.d(TAG, "updateTotalSteps: RecibidoDelWear=$newTotalDeviceStepsFromWear, " +
                "UltimoTotalConocidoDelWear=$lastKnownTotalWearSteps, " +
                "PasosDelWearAcumuladosHoy=$totalStepsFromWearToday")

        var stepsDeltaFromWear = 0L

        if (newTotalDeviceStepsFromWear < lastKnownTotalWearSteps) {
            // Caso 1: El wearable probablemente se reinició (su contador total disminuyó).
            // Consideramos todos los `newTotalDeviceStepsFromWear` como los nuevos pasos desde su reinicio.
            Log.w(TAG, "El contador total del Wearable disminuyó (posible reinicio del Wearable). " +
                    "NuevoTotal: $newTotalDeviceStepsFromWear, UltimoConocido: $lastKnownTotalWearSteps")
            stepsDeltaFromWear = newTotalDeviceStepsFromWear
            // Si el wearable se reinicia, los pasos acumulados hoy provenientes del wearable
            // deberían reflejar este nuevo conteo desde el reinicio del wearable,
            // en lugar de sumar a un acumulado previo al reinicio del wearable.
            // Entonces, `totalStepsFromWearToday` se convierte en este delta.
            totalStepsFromWearToday = stepsDeltaFromWear
        } else if (newTotalDeviceStepsFromWear > lastKnownTotalWearSteps) {
            // Caso 2: El wearable ha contado más pasos desde la última vez.
            stepsDeltaFromWear = newTotalDeviceStepsFromWear - lastKnownTotalWearSteps
            totalStepsFromWearToday += stepsDeltaFromWear // Sumar el delta a los acumulados de hoy
        }
        // Si newTotalDeviceStepsFromWear == lastKnownTotalWearSteps, entonces stepsDeltaFromWear es 0.
        // No hay nuevos pasos del wearable, `totalStepsFromWearToday` no cambia.

        if (stepsDeltaFromWear > 0 || (newTotalDeviceStepsFromWear < lastKnownTotalWearSteps && newTotalDeviceStepsFromWear >= 0) ) {
            // Guardar solo si hubo un cambio o un reinicio detectado
            sharedPreferences.edit()
                .putLong(PREF_LAST_KNOWN_TOTAL_WEAR_STEPS, newTotalDeviceStepsFromWear)
                .putLong(PREF_TOTAL_STEPS_FROM_WEAR_TODAY, totalStepsFromWearToday)
                .apply()

            Log.i(TAG, "Pasos del Wearable actualizados para hoy: $totalStepsFromWearToday. " +
                    "(Delta: $stepsDeltaFromWear). UltimoTotalDelWear Guardado: $newTotalDeviceStepsFromWear")

            // Notificar a la UI u otros componentes sobre la actualización
            broadcastStepUpdate(totalStepsFromWearToday)
        } else {
            Log.d(TAG, "No hubo nuevos pasos detectables del wearable o sin cambios significativos.")
        }
    }

    /**
     * Transmite la actualización del conteo de pasos acumulados del wearable para el día de hoy.
     */
    private fun broadcastStepUpdate(stepsTodayFromWear: Long) {
        val intent = Intent(ACTION_WEAR_STEPS_UPDATED)
        intent.putExtra(EXTRA_UPDATED_STEPS_FROM_WEAR_TODAY, stepsTodayFromWear)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "Actualización de pasos del wearable ($stepsTodayFromWear) transmitida localmente.")
    }

    /**
     * Verifica si es un nuevo día y resetea los contadores diarios si es necesario.
     */
    private fun resetDailyCountersIfNeeded() {
        val todayCalendar = Calendar.getInstance()
        val currentDayOfYear = todayCalendar.get(Calendar.DAY_OF_YEAR)
        val lastResetDay = sharedPreferences.getInt(PREF_LAST_RESET_DAY_OF_YEAR, -1)

        if (currentDayOfYear != lastResetDay) {
            Log.i(TAG, "Nuevo día detectado ($currentDayOfYear). Reseteando contadores diarios de pasos del wearable.")
            sharedPreferences.edit()
                .putLong(PREF_TOTAL_STEPS_FROM_WEAR_TODAY, 0L) // Resetea los acumulados del wearable para el nuevo día
                .putLong(PREF_LAST_KNOWN_TOTAL_WEAR_STEPS, 0L) // Resetea el último total conocido del wearable,
                // para que el primer dato del wearable en el nuevo día
                // establezca la base o se considere un delta completo.
                .putInt(PREF_LAST_RESET_DAY_OF_YEAR, currentDayOfYear)
                .apply()
        }
    }
}


//class DataLayerService : WearableListenerService() {
//
//    companion object {
//        const val STEP_DATA_PATH = "/step-data"
//        const val TAG = "DataLayerService"
//    }
//
//    override fun onDataChanged(dataEvents: DataEventBuffer) {
//        super.onDataChanged(dataEvents)
//
//        for (event in dataEvents) {
//            if (event.type == DataEvent.TYPE_CHANGED) {
//                val dataItem = event.dataItem
//                if (dataItem.uri.path == STEP_DATA_PATH) {
//                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
//                    val stepsFromWear = dataMap.getInt("steps", 0)
//                    val timestamp = dataMap.getLong("timestamp", System.currentTimeMillis())
//
//                    // Actualizar los pasos totales
//                    updateTotalSteps(stepsFromWear)
//                }
//            }
//        }
//    }
//
//    override fun onMessageReceived(messageEvent: MessageEvent) {
//        super.onMessageReceived(messageEvent)
//
//        when (messageEvent.path) {
//            "/step-update" -> {
//                val stepsData = String(messageEvent.data)
//                Log.d(TAG, "Received step update: $stepsData")
//                // Procesar datos de pasos del wearable
//            }
//        }
//    }
//
//    private fun updateTotalSteps(wearSteps: Int) {
//        // Esta función será llamada cuando se reciban datos del wearable
//        // Deberías implementar la lógica para combinar los pasos del móvil y el wearable
//        Log.d(TAG, "Steps from wear: $wearSteps")
//    }
//}