package com.example.stepupquest_tv

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.add
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.text
import androidx.compose.ui.tooling.data.position
import androidx.core.text.color
import androidx.fragment.app.FragmentActivity // O AppCompatActivity si la usas
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import kotlin.text.clear


// Clases de datos que esperas recibir (deben coincidir con las del móvil)
data class StatsHistoryMessage(val type: String, val data: Map<String, Int>)


class TvStatsActivity : FragmentActivity() {

    private lateinit var barChart: BarChart
    private lateinit var textViewChartTitle: TextView
    private lateinit var buttonDaily: Button
    private lateinit var buttonWeekly: Button
    private var castSession: CastSession? = null
    private val gson = Gson() // Initialize Gson

    // ... (otras variables como barChart, textViewChartTitle, buttons, castSession, gson)

    // Almacenará el historial completo recibido del móvil
    private var fullStepHistory: Map<String, Int>? = null

    // ... (sessionManagerListener y companion object como antes)
    // Asegúrate que STATS_NAMESPACE sea el mismo que usa tu TVDataSender.kt (`urn:x-cast:com.utnay.stepupquest`)
    companion object {
        private const val TAG = "TvStatsActivity"
        // Este es el namespace que usa tu TVDataSender.kt
        private const val STATS_NAMESPACE = "urn:x-cast:com.utnay.stepupquest"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_stats)

        textViewChartTitle = findViewById(R.id.textViewChartTitle)
        barChart = findViewById(R.id.barChartStats)
        buttonDaily = findViewById(R.id.buttonDaily)
        buttonWeekly = findViewById(R.id.buttonWeekly)

        setupChart()
        com.google.android.gms.cast.framework.CastContext.getSharedInstance(this) // Asegura inicialización

        buttonDaily.setOnClickListener {
            textViewChartTitle.text = "Pasos Diarios (Últimos 7 días)"
            fullStepHistory?.let { history ->
                val dailyData = TvStatsProcessor.getDailyChartDataFromHistory(history) // Usa el procesador
                displayChartData(dailyData)
            } ?: Log.w(TAG, "Historial de pasos aún no disponible para datos diarios.")
        }

        buttonWeekly.setOnClickListener {
            textViewChartTitle.text = "Pasos Semanales"
            fullStepHistory?.let { history ->
                val weeklyData = TvStatsProcessor.getWeeklyChartDataFromHistory(history) // Usa el procesador
                displayChartData(weeklyData)
            } ?: Log.w(TAG, "Historial de pasos aún no disponible para datos semanales.")
        }
        // Mostrar la vista diaria por defecto si hay datos al inicio
        // Esto se podría llamar después de recibir los datos la primera vez.
    }

    private fun setupChart() {
        fun setupChart() {
            barChart.setDrawBarShadow(false)        // No dibujar sombra detrás de las barras
            barChart.setDrawValueAboveBar(true)   // Dibujar valores encima de las barras
            barChart.description.isEnabled = false  // Deshabilitar la descripción del gráfico (texto abajo a la derecha)
            barChart.setPinchZoom(false)          // Deshabilitar el zoom con gesto de pellizco
            barChart.isDoubleTapToZoomEnabled = false // Deshabilitar zoom con doble toque
            barChart.setDrawGridBackground(false) // No dibujar el fondo de la cuadrícula

            // Configuración del Eje X (horizontal - etiquetas de días/semanas)
            val xAxis = barChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM // Posicionar las etiquetas del eje X abajo
            xAxis.setDrawGridLines(false)           // No dibujar las líneas de la cuadrícula vertical
            xAxis.granularity = 1f                  // Intervalo mínimo entre valores del eje X (importante para IndexAxisValueFormatter)
            xAxis.setDrawAxisLine(true)             // Dibujar la línea del eje X
            // xAxis.valueFormatter será establecido en displayChartData cuando tengamos las etiquetas

            // Configuración del Eje Y Izquierdo (vertical - valores de pasos)
            val leftAxis = barChart.axisLeft
            leftAxis.setDrawGridLines(true)            // Dibujar líneas de cuadrícula horizontal desde el eje izquierdo
            leftAxis.axisMinimum = 0f                  // Empezar el eje Y en 0
            // leftAxis.axisMaximum = ... // Podrías establecer un máximo si lo deseas, o dejar que se calcule automáticamente

            // Configuración del Eje Y Derecho
            barChart.axisRight.isEnabled = false       // Deshabilitar el eje Y derecho (a menudo no es necesario)

            // Leyenda del gráfico (cuadrito que dice "Pasos" con el color)
            val legend = barChart.legend
            legend.isEnabled = true                   // Habilitar la leyenda
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            // Para asegurar que el gráfico se redibuje si ya tiene datos (poco probable aquí, pero buena práctica)
            barChart.invalidate()
        }
    }

    // ... (onResume, onPause, attachMessageReceiver como antes)

    inner class CastMessageReceiver : com.google.android.gms.cast.Cast.MessageReceivedCallback {
        override fun onMessageReceived(castDevice: com.google.android.gms.cast.CastDevice, namespace: String, message: String) {
            Log.d(TAG, "Mensaje recibido de ${castDevice.friendlyName} en namespace $namespace: $message")

            if (namespace == STATS_NAMESPACE) {
                try {
                    // Primero, intenta deserializar como StatsHistoryMessage
                    // Tu TVDataSender para 'stats_data' envía: mapOf("type" to "stats_data", "data" to statsData)
                    val historyMessageType = object : TypeToken<StatsHistoryMessage>() {}.type
                    val receivedMessage = gson.fromJson<StatsHistoryMessage>(message, historyMessageType)

                    if (receivedMessage.type == "stats_data") {
                        fullStepHistory = receivedMessage.data
                        Log.i(TAG, "Historial completo de pasos recibido y almacenado (${fullStepHistory?.size} entradas).")
                        // Opcional: Actualizar el gráfico inmediatamente con una vista por defecto (ej. diaria)
                        runOnUiThread {
                            // Si es la primera vez que se reciben datos, o si quieres refrescar la vista actual:
                            if (buttonDaily.hasFocus() || textViewChartTitle.text.toString().contains("Diarios") || barChart.data == null) { // Condición de ejemplo
                                fullStepHistory?.let {
                                    textViewChartTitle.text = "Pasos Diarios (Últimos 7 días)"
                                    val dailyData = TvStatsProcessor.getDailyChartDataFromHistory(it)
                                    displayChartData(dailyData)
                                }
                            } else if (buttonWeekly.hasFocus() || textViewChartTitle.text.toString().contains("Semanales")) {
                                fullStepHistory?.let {
                                    textViewChartTitle.text = "Pasos Semanales"
                                    val weeklyData = TvStatsProcessor.getWeeklyChartDataFromHistory(it)
                                    displayChartData(weeklyData)
                                }
                            }
                        }
                    } else if (receivedMessage.type == "step_data") { // Si también manejas el StepData aquí
                        // Lógica para StepData (el que envías en sendDataToTV con pasos actuales, etc.)
                        // Esto es si tu namespace STATS_NAMESPACE también recibe estos mensajes.
                        // Deberías tener una clase de datos para StepData también.
                        // val stepData = gson.fromJson<StepData>(message, StepData::class.java)
                        Log.d(TAG, "StepData (no de historial) recibido: ${receivedMessage.type}")
                        // Aquí podrías actualizar otro TextView con los pasos actuales, si lo tienes en esta pantalla.
                    }
                    else {
                        Log.w(TAG, "Tipo de mensaje desconocido dentro del namespace: ${receivedMessage.type}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear JSON recibido: $message", e)
                }
            }
        }
    }

    // ... (setupChart como antes)

    // Modificar displayChartData para que acepte List<TvChartDataPoint>
    private fun displayChartData(dataPoints: List<TvChartDataPoint>) { // Cambiado el tipo de parámetro
        if (dataPoints.isEmpty()) {
            barChart.data?.clearValues() // Limpiar datos existentes si hay
            barChart.xAxis.valueFormatter = null // Limpiar formateador antiguo
            barChart.notifyDataSetChanged() // Notificar al gráfico sobre el cambio
            barChart.invalidate()
            Log.d(TAG, "No hay datos para mostrar en el gráfico.")
            // Podrías mostrar un TextView con "No hay datos"
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        dataPoints.forEachIndexed { index, dataPoint ->
            entries.add(BarEntry(index.toFloat(), dataPoint.value))
            labels.add(dataPoint.label)
        }

        val dataSet = BarDataSet(entries, "Pasos")
        dataSet.color = android.graphics.Color.rgb(63, 81, 181) // Ejemplo
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size.coerceAtLeast(1) // Evitar crash si labels está vacío pero entries no (no debería pasar)
        barChart.xAxis.granularity = 1f

        barChart.description.text = "" // Limpiar descripción por si acaso
        barChart.notifyDataSetChanged() // Importante notificar al gráfico
        barChart.invalidate() // Refrescar el gráfico
        barChart.animateY(1000)
    }
}
