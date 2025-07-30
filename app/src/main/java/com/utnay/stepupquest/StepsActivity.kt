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
    private var totalSteps = 0

    private lateinit var textViewSteps: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnEst: Button
    private lateinit var btnCleanSteps: Button
    private lateinit var btnMeta: Button

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var accelEvent: SensorEvent? = null
    private var gyroEvent: SensorEvent? = null

    private lateinit var sensorFusionHelper: SensorFusionHelper
    private lateinit var stepDataManager: StepDataManager
    private lateinit var dataStorageManager: DataStorageManager
    private lateinit var tvDataSender: TVDataSender

    private val setGoalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getIntExtra("newGoal", 10000)?.let { updateGoal(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)

        // Inicializar vistas
        textViewSteps = findViewById(R.id.textViewSteps)
        textViewGoal = findViewById(R.id.textViewGoal)
        progressBar = findViewById(R.id.progressBar)
        btnEst = findViewById(R.id.btnEst)
        btnCleanSteps = findViewById(R.id.btnCleanSteps)
        btnMeta = findViewById(R.id.btnMeta)

        // Inicializar sensores y helpers
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorFusionHelper = SensorFusionHelper()

        // Inicializar gestores
        stepDataManager = StepDataManager(this)
        dataStorageManager = DataStorageManager(this)
        tvDataSender = TVDataSender(this)

        // Cargar valores previos
        dailyGoal = dataStorageManager.getDailyGoal()
        stepCount = dataStorageManager.getTodaySteps()
        totalSteps = stepCount

        // Mostrar datos iniciales
        updateStepCount(stepCount)
        updateGoal(dailyGoal)

        // Botón: ver estadísticas
        btnEst.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            intent.putExtra("dailyGoal", dailyGoal)
            startActivity(intent)
        }

        // Botón: reiniciar pasos
        btnCleanSteps.setOnClickListener {
            stepCount = 0
            updateStepCount(stepCount)
            Toast.makeText(this, "Contador reiniciado", Toast.LENGTH_SHORT).show()
        }

        // Botón: cambiar meta
        btnMeta.setOnClickListener {
            val intent = Intent(this, GoalActivity::class.java)
            intent.putExtra("currentGoal", dailyGoal)
            setGoalLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No utilizado
    }

    private fun checkForStep() {
        if (sensorFusionHelper.isStepDetected(accelEvent, gyroEvent)) {
            stepCount++
            totalSteps = stepCount
            updateStepCount(stepCount)
            stepDataManager.sendStepsToWear(totalSteps)
            checkMilestonePercentage()
            sendDataToTV()
        }
    }

    private fun checkMilestonePercentage() {
        val percentage = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal
        when (percentage) {
            in 50..54 -> {
                stepDataManager.sendNotificationToWear(percentage, "¡Llegaste al 50% de tu meta diaria!")
                sendStatsToTV()
            }
            in 80..84 -> {
                stepDataManager.sendNotificationToWear(percentage, "¡Llegaste al 80% de tu meta diaria!")
                sendStatsToTV()
            }
        }
    }

    private fun updateStepCount(steps: Int) {
        stepCount = steps
        textViewSteps.text = getString(R.string.steps_text, stepCount)
        updateProgressBar()
        dataStorageManager.saveDailySteps(stepCount)
        dataStorageManager.addToHistory(stepCount)

        Log.d("StepsActivity", "Pasos: $stepCount | Historial: ${dataStorageManager.getStepHistory()}")
    }

    private fun updateGoal(goal: Int) {
        dailyGoal = goal
        textViewGoal.text = getString(R.string.goal_text, dailyGoal)
        updateProgressBar()
        dataStorageManager.saveDailyGoal(goal)
    }

    private fun updateProgressBar() {
        val percentage = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal
        progressBar.progress = percentage.coerceAtMost(100)
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
            val success = tvDataSender.sendDataToTV(stepData)
            Log.d("StepsActivity", "TV: datos enviados ${if (success) "✔" else "✘"}")
        }
    }

    private fun sendStatsToTV() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = dataStorageManager.getStepHistory()
            val success = tvDataSender.sendStatsData(history)
            Log.d("StepsActivity", "TV: historial enviado ${if (success) "✔" else "✘"}")
        }
    }
}
