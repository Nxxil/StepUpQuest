package com.utnay.stepupquest

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var stepCount = 0
    private var dailyGoal = 10000
    private var lastAcceleration = 0f
    private var lastTime: Long = 0
    private var totalSteps = 0 // Pasos totales (móvil + wearable)

    // Declaración de vistas
    private lateinit var textViewSteps: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMeta: Button
    private lateinit var btnEst: Button

    // Sensores
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Data Manager para comunicación con wearable
    private lateinit var stepDataManager: StepDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vinculación de vistas con sus IDs correctos
        textViewSteps = findViewById(R.id.textViewSteps)
        textViewGoal = findViewById(R.id.textViewGoal)
        progressBar = findViewById(R.id.progressBar)
        btnMeta = findViewById(R.id.btnMeta)
        btnEst = findViewById(R.id.btnEst)

        // Inicializar sensores
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Inicializar Data Manager
        stepDataManager = StepDataManager(this)

        // Inicializar UI
        updateStepCount(stepCount)
        updateGoal(dailyGoal)

        // Botón: Configurar Meta
        btnMeta.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("currentGoal", dailyGoal)
            startActivityForResult(intent, REQUEST_CODE_SET_GOAL)
        }

        // Botón: Ver Estadísticas
        btnEst.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar el listener del sensor
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el listener del sensor para ahorrar batería
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Calcular la magnitud de la aceleración
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val acceleration = sqrt(x * x + y * y + z * z)
                val currentTime = System.currentTimeMillis()

                // Detectar movimiento significativo (paso)
                if ((currentTime - lastTime) > 100) { // Evitar detecciones muy rápidas
                    val diff = kotlin.math.abs(acceleration - lastAcceleration)
                    if (diff > 2.0) { // Umbral de detección de paso
                        stepCount++
                        totalSteps = stepCount // Por ahora solo pasos del móvil
                        updateStepCount(stepCount)

                        // Enviar datos al wearable
                        stepDataManager.sendStepsToWear(totalSteps)

                        // Verificar porcentajes para notificaciones
                        checkMilestonePercentage()
                    }
                    lastAcceleration = acceleration
                    lastTime = currentTime
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita implementar para este caso
    }

    private fun checkMilestonePercentage() {
        val percentage = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal

        when {
            percentage >= 80 && percentage < 85 -> {
                stepDataManager.sendNotificationToWear(percentage, "¡Llegaste al 80% de tu meta diaria!")
            }
            percentage >= 50 && percentage < 55 -> {
                stepDataManager.sendNotificationToWear(percentage, "¡Llegaste al 50% de tu meta diaria!")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SET_GOAL && resultCode == RESULT_OK) {
            data?.getIntExtra("newGoal", 10000)?.let { newGoal ->
                updateGoal(newGoal)
            }
        }
    }

    private fun updateStepCount(steps: Int) {
        stepCount = steps
        textViewSteps.text = "$steps pasos"
        updateProgressBar()
    }

    private fun updateGoal(goal: Int) {
        dailyGoal = goal
        textViewGoal.text = "META: $goal pasos"
        updateProgressBar()
    }

    private fun updateProgressBar() {
        val percentage = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal
        progressBar.progress = percentage.coerceAtMost(100) // Limitar a 100%
    }

    // Metodo para actualizar pasos cuando se reciben datos del wearable
    fun updateStepsFromWear(wearSteps: Int) {
        totalSteps = stepCount + wearSteps
        runOnUiThread {
            updateStepCount(stepCount) // Actualiza la UI con los pasos del móvil
            // El total se maneja internamente para estadísticas y notificaciones
        }
    }

    companion object {
        private const val REQUEST_CODE_SET_GOAL = 1
    }
}