package com.utnay.stepupquest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var btnStream: Button
    private lateinit var btnBack: Button
    private lateinit var dataStorageManager: DataStorageManager
    private var dailyGoal: Int = 10000

    private lateinit var weeklyData: Map<String, Int>

    // Cast
    private lateinit var tvDataSender: TVDataSender
    private lateinit var castContext: CastContext
    private var castSession: CastSession? = null
    private val gson = Gson()
    private val namespace = "urn:x-cast:com.utnay.stepupquest"
    private var customChannel: CustomChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        dailyGoal = intent.getIntExtra("dailyGoal", 10000)

        barChart = findViewById(R.id.barChart)
        btnStream = findViewById(R.id.btnStream)
        btnBack = findViewById(R.id.btnBack)

        dataStorageManager = DataStorageManager(this)
        loadAndDisplayStats()

        btnBack.setOnClickListener { finish() }

        try {
            castContext = CastContext.getSharedInstance(this)
            castContext.sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        } catch (e: Exception) {
            Log.e("StatsActivity", "CastContext error", e)
            Toast.makeText(this, "Cast no disponible", Toast.LENGTH_SHORT).show()
        }

        btnStream.setOnClickListener {
            val session = castContext.sessionManager.currentCastSession
            if (session != null && session.isConnected) {
                sendStatsToTV(weeklyData)
            } else {
                Toast.makeText(this, "No hay dispositivo TV conectado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayStats()
    }

    override fun onDestroy() {
        castContext.sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        super.onDestroy()
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            setupChannel()
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            setupChannel()
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            customChannel = null
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
    }

    private fun setupChannel() {
        try {
            customChannel = CustomChannel(namespace)
            customChannel?.let { channel ->
                castSession?.setMessageReceivedCallbacks(namespace, channel)
            }
        } catch (e: Exception) {
            Log.e("StatsActivity", "Error al registrar canal", e)
        }
    }

    private fun sendStatsToTV(data: Map<String, Int>) {
        val session = castSession
        if (session == null || customChannel == null) return

        try {
            val message = mapOf("type" to "stats_data", "data" to data)
            val json = gson.toJson(message)

            session.sendMessage(namespace, json).setResultCallback { status ->
                runOnUiThread {
                    if (status.isSuccess) {
                        Toast.makeText(this, "Enviado a TV", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al enviar (${status.statusCode})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StatsActivity", "Error al enviar mensaje", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAndDisplayStats() {
        weeklyData = dataStorageManager.getWeeklySteps()
        setupBarChart(weeklyData)
    }

    private fun setupBarChart(data: Map<String, Int>) {
        barChart.clear()
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var maxSteps = 0

        val sortedData = data.toList().sortedBy { (date, _) ->
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
            } catch (e: Exception) {
                Date(0)
            }
        }

        sortedData.forEachIndexed { index, (date, steps) ->
            labels.add(formatDateLabel(date))
            maxSteps = maxOf(maxSteps, steps)
            entries.add(BarEntry(index.toFloat(), steps.toFloat()))
        }

        if (entries.isEmpty()) {
            maxSteps = 10000
            entries.add(BarEntry(0f, 5000f))
            labels.add("N/A")
        }

        val dataSet = BarDataSet(entries, "Pasos")
        dataSet.color = getColorFromRes(R.color.normal_bar_color)
        dataSet.valueTextColor = android.graphics.Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f
        barChart.data = barData

        barChart.description.isEnabled = false
        barChart.legend.textSize = 14f
        barChart.legend.textColor = android.graphics.Color.BLACK

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.textColor = android.graphics.Color.BLACK

        val leftAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxSteps.toFloat()
        leftAxis.textColor = android.graphics.Color.BLACK

        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun formatDateLabel(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString)
            val displayFormat = SimpleDateFormat("EEE dd", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateString.takeLast(5)
        }
    }

    private fun getColorFromRes(colorRes: Int): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                resources.getColor(colorRes, theme)
            } else {
                @Suppress("DEPRECATION")
                resources.getColor(colorRes)
            }
        } catch (e: Exception) {
            android.graphics.Color.BLUE
        }
    }

    inner class CustomChannel(private val namespace: String) : Cast.MessageReceivedCallback {
        override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
            Log.d("StatsActivity", "Mensaje recibido desde TV: $message")
        }

        fun getNamespace(): String = namespace
    }
}
