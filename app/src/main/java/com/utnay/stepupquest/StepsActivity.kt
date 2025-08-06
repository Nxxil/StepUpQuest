package com.utnay.stepupquest

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.app.ActivityCompat
import com.utnay.stepupquest.R.id
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

import android.app.NotificationChannel // Make sure this line is present

import androidx.core.content.ContextCompat


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
    private lateinit var btnProyectarTV: Button

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
        textViewSteps = findViewById(id.textViewSteps)
        textViewGoal = findViewById(id.textViewGoal)
        progressBar = findViewById(id.progressBar)
        btnEst = findViewById(id.btnEst)
        btnCleanSteps = findViewById(id.btnCleanSteps)
        btnMeta = findViewById(id.btnMeta)
        btnProyectarTV = findViewById(id.btnProyectarTV) // Inicializar nuevo botón

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

        // Botón: proyectar a TV
        btnProyectarTV.setOnClickListener {
            proyectarEstadisticasATV()
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.channel_name_progress) // Define en strings.xml
                val descriptionText = context.getString(R.string.channel_description_progress) // Define en strings.xml
                val importance = NotificationManager.IMPORTANCE_LOW // O DEFAULT
                // Complete the NotificationChannel constructor
                val channel = NotificationChannel(CHANNEL_ID_PROGRESS, name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun requestPostNotificationsPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_POST_NOTIFICATIONS
                    )
                }
            }
        }

       // createNotificationChannel(applicationContext)
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

    private fun updateStepsUI(steps: Int) {
        textViewSteps.text = "$steps"
        progressBar.progress = steps
        // ... más lógica de UI ...

        // Aquí es un buen lugar para actualizar la notificación
        val dailyGoal = 10000 // O como obtengas tu meta diaria
        showProgressNotification(this, steps, dailyGoal)
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

    // Nuevo metodo para proyectar estadísticas a la TV
    private fun proyectarEstadisticasATV() {
        CoroutineScope(Dispatchers.IO).launch {
            // Obtener datos de estadísticas actuales
            val weeklySteps = dataStorageManager.getWeeklySteps()
            val fullStats = StepStatsData(
                dailySteps = weeklySteps,
                dailyGoal = dailyGoal,
                currentSteps = stepCount,
                totalSteps = totalSteps // Asegúrate de calcular este valor correctamente
            )
            // Enviar datos a la TV
            val success = tvDataSender.sendFullStatsToTV(fullStats)
            // Opcional: Mostrar un mensaje en la UI principal si falla
            if (!success) {
                Log.w("StepsActivity", "No se pudo enviar los datos a la TV. ¿Está conectada?")
                // Puedes usar runOnUiThread para mostrar un Toast si lo deseas
            } else {
                Log.d("StepsActivity", "Estadísticas proyectadas a la TV exitosamente")
            }
        }
    }
}

private const val CHANNEL_ID_PROGRESS = "step_progress_channel"
private const val NOTIFICATION_ID_PROGRESS = 123
private val REQUEST_CODE_POST_NOTIFICATIONS = 101

private fun showProgressNotification(context: Context, currentSteps: Int, goalSteps: Int) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        // Solicitar permiso si no está concedido (necesario para API 33+)
        // ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), YOUR_REQUEST_CODE) // If you need to request permission
        Log.w("Notification", "Permiso de notificación no concedido.")
        return
    }

    val percentage = if (goalSteps > 0) (currentSteps * 100 / goalSteps) else 0
    val contentText = "Llevas un $percentage% de tu meta ($currentSteps/$goalSteps pasos)"

    val builder = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
        .setSmallIcon(R.drawable.checkflag) // Reemplaza con tu icono
        .setContentTitle("Progreso de Pasos - StepUpQuest")
        .setContentText(contentText)
        .setProgress(goalSteps, currentSteps, false)
        .setPriority(NotificationCompat.PRIORITY_LOW) // O DEFAULT, según prefieras
        .setOngoing(false) // false si el usuario puede descartarla; true si es más persistente
        .setOnlyAlertOnce(true) // Solo alerta la primera vez o si el contenido cambia mucho

    // Use 'context' instead of 'this'
    val intent = Intent(context, StepsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    builder.setContentIntent(pendingIntent)

    // Use 'context' instead of 'this'
    with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID_PROGRESS, builder.build())
    }
}



