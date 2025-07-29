package com.utnay.stepupquest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity3 : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var btnBack: Button
    private lateinit var dataStorageManager: DataStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        // Inicializar vistas
        barChart = findViewById(R.id.barChart)
        btnBack = findViewById(R.id.btnBack)

        // Inicializar DataStorageManager
        dataStorageManager = DataStorageManager(this)

        // Configurar botón de regreso
        btnBack.setOnClickListener {
            finish()
        }

        // Cargar y mostrar estadísticas
        loadAndDisplayStats()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar estadísticas cuando la actividad se reanuda
        loadAndDisplayStats()
    }

    private fun loadAndDisplayStats() {
        // Obtener datos de la semana
        val weeklyData = dataStorageManager.getWeeklySteps()

        // Configurar y mostrar gráfico
        setupBarChart(weeklyData)
    }

    private fun setupBarChart(weeklyData: Map<String, Int>) {
        // Limpiar datos existentes
        barChart.clear()

        // Preparar datos para el gráfico
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        // Convertir mapa a lista y ordenar por fecha
        val sortedData = weeklyData.toList().sortedBy {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.first)
            } catch (e: Exception) {
                Date() // Fecha actual si hay error
            }
        }

        // Crear entradas para el gráfico
        sortedData.forEachIndexed { index, (date, steps) ->
            entries.add(BarEntry(index.toFloat(), steps.toFloat()))
            labels.add(formatDateLabel(date))
        }

        // Si no hay datos reales, mostrar datos de prueba
        if (entries.isEmpty()) {
            // Generar datos de prueba para los últimos 7 días
            val calendar = Calendar.getInstance()
            for (i in 6 downTo 0) {
                val testDate = calendar.clone() as Calendar
                testDate.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(testDate.time)

                // Generar pasos aleatorios entre 1000 y 10000
                val randomSteps = (1000..10000).random().toFloat()

                entries.add(BarEntry((6-i).toFloat(), randomSteps))
                labels.add(formatDateLabel(dateStr))
            }
        }

        // Crear dataset
        val dataSet = BarDataSet(entries, "Pasos por día")
        dataSet.color = getColorFromRes(R.color.chart_bar_color)
        dataSet.valueTextColor = android.graphics.Color.BLACK
        dataSet.valueTextSize = 12f

        // Crear BarData
        val barData = BarData(dataSet)
        barData.barWidth = 0.7f

        // Configurar gráfico
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.legend.textSize = 14f
        barChart.legend.textColor = android.graphics.Color.BLACK

        // Configurar eje X
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 10f
        xAxis.textColor = android.graphics.Color.BLACK

        // Configurar eje Y izquierdo
        val leftAxis = barChart.axisLeft
        leftAxis.textSize = 10f
        leftAxis.textColor = android.graphics.Color.BLACK
        leftAxis.axisMinimum = 0f

        // Ocultar eje Y derecho
        barChart.axisRight.isEnabled = false

        // Animación
        barChart.animateY(1000)

        // Refrescar
        barChart.invalidate()
    }

    private fun formatDateLabel(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString)
            val displayFormat = SimpleDateFormat("EEE dd", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateString.takeLast(5) // Retorna solo mes-día si falla el parseo
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
            android.graphics.Color.BLUE // Color por defecto
        }
    }
}