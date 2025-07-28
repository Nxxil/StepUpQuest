package com.utnay.stepupquest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry


class MainActivity3 : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        barChart = findViewById(R.id.barChart)
        btnBack = findViewById(R.id.btnBack)

        // Datos simulados: últimos 7 días
        val stepsData = listOf(5200, 8100, 6300, 10500, 7200, 9800, 6700)
        val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        setupBarChart(stepsData, days)

        // Botón para volver
        btnBack.setOnClickListener {
            finish() // Cierra esta actividad y regresa a MainActivity
        }
    }

    private fun setupBarChart(steps: List<Int>, labels: List<String>) {
        val entries = ArrayList<BarEntry>()
        for (i in steps.indices) {
            entries.add(BarEntry(i.toFloat(), steps[i].toFloat()))
        }

        val dataSet = BarDataSet(entries, "Pasos")
        dataSet.color = Color.parseColor("#3c7966")
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f

        barChart.data = barData
        barChart.description.text = "Pasos por día"
        barChart.description.textSize = 12f
        barChart.setTouchEnabled(true)
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(false)
        barChart.axisRight.isEnabled = false

        // Configurar eje X
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size
        xAxis.setCenterAxisLabels(true)  // ✅ Método correcto

        // Ajuste obligatorio para centrar etiquetas
        barChart.xAxis.axisMinimum = -0.5f
        barChart.xAxis.axisMaximum = labels.size - 0.5f

        // Configurar eje Y
        val yAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.setLabelCount(6, true)

        barChart.invalidate() // Refresca el gráfico
    }
}

