/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.stepupquest_wear.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.stepupquest_wear.R
import com.example.stepupquest_wear.presentation.theme.StepUpQuestTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.compose.ui.semantics.text
import androidx.fragment.app.FragmentActivity // O androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.ui.input.key.type
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.values
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlin.text.toLong

class MainActivity : FragmentActivity(), SensorEventListener {

    // Para la hora
    private lateinit var tvHora: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    // Para el contador de pasos
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var totalStepsOnDeviceSinceReboot: Long = 0L // Pasos totales del dispositivo
    private var stepsAtRegistration: Long = -1L // Pasos cuando el listener se registró
    private var currentStepsInSession: Long = 0L // Pasos contados en esta "sesión" de la app

    private lateinit var tvPasos: TextView // TextView con id "tvPasos"

    companion object {
        private const val TAG = "WearMainActivity"
        private const val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
        const val WEAR_STEP_DATA_PATH = "/wear-step-data"
        const val KEY_STEPS = "steps"
        const val KEY_TIMESTAMP = "timestamp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar UI para la hora
        tvHora = findViewById(R.id.tvHora)
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateClock()
                handler.postDelayed(this, 60000)
            }
        }

        // Inicializar UI para los pasos
        tvPasos = findViewById(R.id.tvPasos) // Asegúrate de tener este TextView en tu XML
        tvPasos.text = "Pasos: 0" // Valor inicial

        // Inicializar SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor == null) {
            tvPasos.text = "Sensor no disponible"
            Log.e(TAG, "Sensor de contador de pasos no disponible.")
        }
        // La solicitud de permisos y el registro del listener se harán en onResume

        val recyclerView = findViewById<WearableRecyclerView>(R.id.recyclerViewNotificaciones)
// Aquí podrías inicializar el adapter
    }

    override fun onResume() {
        super.onResume()
        // Iniciar actualización de la hora
        handler.post(timeUpdateRunnable)

        // Registrar listener del sensor de pasos
        if (stepCounterSensor != null) {
            if (checkAndRequestActivityRecognitionPermission()) {
                Log.d(TAG, "Registrando listener del sensor de pasos.")
                // SENSOR_DELAY_NORMAL es un buen compromiso para UI.
                // Para conteo en segundo plano considera SENSOR_DELAY_UI o incluso tasas más lentas
                // o usar batching si es un Service.
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            Log.w(TAG, "No se puede registrar el listener: sensor de pasos nulo.")
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener actualización de la hora
        handler.removeCallbacks(timeUpdateRunnable)

        // Desregistrar listener del sensor para ahorrar batería
        if (stepCounterSensor != null) {
            Log.d(TAG, "Desregistrando listener del sensor de pasos.")
            sensorManager.unregisterListener(this)
        }
        // Reseteamos stepsAtRegistration para que la próxima vez que se registre,
        // comience a contar los pasos de esa nueva sesión.
        stepsAtRegistration = -1L
    }

    // --- Implementación de SensorEventListener ---
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                totalStepsOnDeviceSinceReboot = it.values[0].toLong()
                Log.d(TAG, "SensorChanged - Pasos totales del dispositivo: $totalStepsOnDeviceSinceReboot")

                if (stepsAtRegistration == -1L) {
                    // Esta es la primera lectura después de registrar el listener
                    // o después de un onPause/onResume.
                    stepsAtRegistration = totalStepsOnDeviceSinceReboot
                    Log.d(TAG, "Primera lectura de pasos en esta sesión: $stepsAtRegistration")
                }

                // Calcula los pasos dados desde que el listener se registró en esta sesión
                currentStepsInSession = totalStepsOnDeviceSinceReboot - stepsAtRegistration
                tvPasos.text = "Pasos: $currentStepsInSession"
                Log.d(TAG, "Pasos en esta sesión: $currentStepsInSession")

                // Envía los pasos de la sesión actual al teléfono
                sendStepsToPhone(currentStepsInSession)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión del sensor cambiada a: $accuracy")
        // Puedes querer hacer algo si la precisión es baja, pero para el contador de pasos
        // normalmente no es necesario.
    }

    // --- Lógica para enviar datos al móvil ---
    private fun sendStepsToPhone(steps: Long) {
        val putDataMapReq = PutDataMapRequest.create(WEAR_STEP_DATA_PATH)
        putDataMapReq.dataMap.putLong(KEY_STEPS, steps) // Enviamos los pasos de la sesión
        putDataMapReq.dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        putDataMapReq.setUrgent() // Opcional, para una sincronización más rápida

        val putDataReq = putDataMapReq.asPutDataRequest()

        Wearable.getDataClient(applicationContext).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Datos de pasos ($steps) enviados al teléfono.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al enviar datos de pasos al teléfono.", e)
            }
    }

    // --- Lógica para la hora (código anterior) ---
    private fun updateClock() {
        val calendar = Calendar.getInstance()
        val is24HourFormat = android.text.format.DateFormat.is24HourFormat(this)
        val pattern = if (is24HourFormat) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val currentTime = sdf.format(calendar.time)
        tvHora.text = currentTime
    }

    // --- Manejo de Permisos (para API 30+) ---
    private fun checkAndRequestActivityRecognitionPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30 (Android 11 / Wear OS 3)
            return if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Permiso ACTIVITY_RECOGNITION ya concedido.")
                true
            } else {
                Log.d(TAG, "Solicitando permiso ACTIVITY_RECOGNITION.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
                false // El permiso no está concedido AÚN, se registrará el listener si se concede
            }
        } else {
            // Para versiones anteriores a API 30, el permiso se concede en la instalación si está en el Manifest
            Log.d(TAG, "Versión de SDK < R, no se requiere solicitud de permiso en tiempo de ejecución.")
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Permiso ACTIVITY_RECOGNITION concedido por el usuario.")
                // Ahora que el permiso está concedido, intenta registrar el listener de nuevo
                // Esto es importante si el usuario concede el permiso después de que onResume ya se llamó.
                if (stepCounterSensor != null) {
                    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
                }
            } else {
                Log.w(TAG, "Permiso ACTIVITY_RECOGNITION denegado por el usuario.")
                tvPasos.text = "Permiso denegado"
                // Manejar el caso de permiso denegado (e.g., mostrar un mensaje)
            }
        }
    }



}



