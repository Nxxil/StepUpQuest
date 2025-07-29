package com.utnay.stepupquest

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepsActivity : AppCompatActivity(), SensorEventListener {

    private var stepCount = 0
    private var dailyGoal = 10000
    private var totalSteps = 0 // Pasos totales (móvil + wearable)

    // Declaración de vistas
    private lateinit var textViewSteps: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnEst: Button
    private lateinit var btnCleanSteps: Button
    private lateinit var btnMeta: Button



    // Sensores
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Variables para eventos de sensores
    private var accelEvent: SensorEvent? = null
    private var gyroEvent: SensorEvent? = null

    // Helper para fusión de sensores
    private lateinit var sensorFusionHelper: SensorFusionHelper

    // Data Manager para comunicación con wearable
    private lateinit var stepDataManager: StepDataManager

    // Managers para almacenamiento y TV
    private lateinit var dataStorageManager: DataStorageManager
    private lateinit var tvDataSender: TVDataSender

    // Activity Result Launcher para configurar meta
    private val setGoalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getIntExtra("newGoal", 10000)?.let { newGoal ->
                updateGoal(newGoal)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)

        // Vinculación de vistas con sus IDs correctos
        textViewSteps = findViewById(R.id.textViewSteps)
        textViewGoal = findViewById(R.id.textViewGoal)
        progressBar = findViewById(R.id.progressBar)
        btnEst = findViewById(R.id.btnEst)
        btnCleanSteps = findViewById(R.id.btnCleanSteps)
        btnMeta = findViewById(R.id.btnMeta)



        // Inicializar sensores
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Inicializar SensorFusionHelper
        sensorFusionHelper = SensorFusionHelper()

        // Inicializar Data Manager
        stepDataManager = StepDataManager(this)

        // Inicializar managers para almacenamiento y TV
        dataStorageManager = DataStorageManager(this)
        tvDataSender = TVDataSender(this)

        // Cargar la meta guardada
        dailyGoal = dataStorageManager.getDailyGoal()
        stepCount = dataStorageManager.getTodaySteps()
        totalSteps = stepCount

        // Inicializar UI
        updateStepCount(stepCount)
        updateGoal(dailyGoal)

        // Botón: Ver Estadísticas
        btnEst.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        // Botón: Limpiar Pasos
        btnCleanSteps.setOnClickListener {
            stepCount = 0
            updateStepCount(stepCount) //Actualiza el contador
            //Mensaje de reinicio
            Toast.makeText(this, "El contador de pasos ha sido reiniciado", Toast.LENGTH_SHORT).show()
        }

        // Botón: Configurar Meta
        btnMeta.setOnClickListener {
            val intent = Intent(this, GoalActivity::class.java)
            intent.putExtra("currentGoal", dailyGoal)
            setGoalLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar los listeners de los sensores
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar los listeners de los sensores para ahorrar batería
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelEvent = it
                    checkForStep()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroEvent = it
                    checkForStep()
                }
            }
        }
    }

    private fun checkForStep() {
        if (sensorFusionHelper.isStepDetected(accelEvent, gyroEvent)) {
            stepCount++
            totalSteps = stepCount
            updateStepCount(stepCount)

            // Enviar datos al wearable
            stepDataManager.sendStepsToWear(totalSteps)

            // Verificar porcentajes para notificaciones
            checkMilestonePercentage()

            // Enviar datos a TV
            sendDataToTV()
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
                sendStatsToTV()
            }
            percentage >= 50 && percentage < 55 -> {
                stepDataManager.sendNotificationToWear(percentage, "¡Llegaste al 50% de tu meta diaria!")
                sendStatsToTV()
            }
        }
    }

    private fun updateStepCount(steps: Int) {
        stepCount = steps
        textViewSteps.text = getString(R.string.steps_text, stepCount)
        updateProgressBar()

        // Guardar pasos diarios y en historial
        dataStorageManager.saveDailySteps(stepCount)
        dataStorageManager.addToHistory(stepCount)

        // Notificar que las estadísticas se actualizaron
        notifyStatsUpdated()

        // Logging para debug
        Log.d("MainActivity", "Pasos actualizados: $stepCount, Historial: ${dataStorageManager.getStepHistory()}")
    }

    private fun updateGoal(goal: Int) {
        dailyGoal = goal
        textViewGoal.text = getString(R.string.goal_text, dailyGoal)
        updateProgressBar()

        // Guardar meta diaria
        dataStorageManager.saveDailyGoal(goal)
    }

    private fun updateProgressBar() {
        val percentage = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal
        progressBar.progress = percentage.coerceAtMost(100) // Limitar a 100%
    }

    private fun sendDataToTV() {
        CoroutineScope(Dispatchers.IO).launch {
            val percentage = if (dailyGoal == 0) 0 else (stepCount * 100) / dailyGoal
            val stepData = StepData(
                mobileSteps = stepCount,
                wearSteps = 0,
                totalSteps = totalSteps,
                timestamp = System.currentTimeMillis(),
                dailyGoal = dailyGoal
            )

            Log.d("MainActivity", "Enviando datos a TV: $stepData")
            val success = tvDataSender.sendDataToTV(stepData)
            Log.d("MainActivity", "Envío a TV ${if (success) "exitoso" else "fallido"}")
        }
    }

    private fun sendStatsToTV() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = dataStorageManager.getStepHistory()
            Log.d("MainActivity", "Enviando estadísticas a TV: $history")
            val success = tvDataSender.sendStatsData(history)
            Log.d("MainActivity", "Envío de estadísticas a TV ${if (success) "exitoso" else "fallido"}")
        }
    }

    // Metodo para notificar actualizaciones de estadísticas
    private fun notifyStatsUpdated() {
        // Enviar broadcast para notificar que las estadísticas han cambiado
        val intent = Intent("com.utnay.stepupquest.STATS_UPDATED")
        intent.putExtra("steps", stepCount)
        sendBroadcast(intent)
    }
}