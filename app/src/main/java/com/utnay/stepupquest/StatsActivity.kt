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
import kotlin.math.max
import kotlin.random.Random

class StatsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var btnBack: Button
    private lateinit var dataStorageManager: DataStorageManager
    private var dailyGoal: Int = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        dailyGoal = intent.getIntExtra("dailyGoal", 10000)

        barChart = findViewById(R.id.barChart)
        btnBack = findViewById(R.id.btnBack)

        dataStorageManager = DataStorageManager(this)

        btnBack.setOnClickListener { finish() }

        loadAndDisplayStats()
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayStats()
    }

    private fun loadAndDisplayStats() {
        val weeklyData = dataStorageManager.getWeeklySteps()
        setupBarChart(weeklyData)
    }

    private fun setupBarChart(weeklyData: Map<String, Int>) {
        barChart.clear()
        val stackedEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var maxSteps = 0

        val sortedData = weeklyData.toList().sortedBy {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.first)
            } catch (e: Exception) {
                Date()
            }
        }

        sortedData.forEachIndexed { index, (date, steps) ->
            labels.add(formatDateLabel(date))
            val goal = dailyGoal.toFloat()
            val stepsFloat = steps.toFloat()
            maxSteps = max(maxSteps, steps)

            if (steps > dailyGoal) {
                stackedEntries.add(BarEntry(index.toFloat(), floatArrayOf(goal, stepsFloat - goal)))
            } else {
                stackedEntries.add(BarEntry(index.toFloat(), floatArrayOf(stepsFloat, 0f)))
            }
        }

        if (stackedEntries.isEmpty()) {
            maxSteps = generateTestData(labels, stackedEntries)
        }

        if (maxSteps == 0) maxSteps = dailyGoal

        val stackedDataSet = BarDataSet(stackedEntries, "Pasos")
        stackedDataSet.setColors(
            getColorFromRes(R.color.normal_bar_color),    // Verde
            getColorFromRes(R.color.plus_ultra_bar_color) // Naranja
        )
        stackedDataSet.stackLabels = arrayOf("Pasos base", "Exceso")
        stackedDataSet.valueTextColor = android.graphics.Color.BLACK
        stackedDataSet.valueTextSize = 12f

        val barData = BarData(stackedDataSet)
        barData.barWidth = 0.7f
        barChart.data = barData

        barChart.description.isEnabled = false
        barChart.legend.textSize = 14f
        barChart.legend.textColor = android.graphics.Color.BLACK

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 10f
        xAxis.textColor = android.graphics.Color.BLACK

        val leftAxis = barChart.axisLeft
        leftAxis.textSize = 10f
        leftAxis.textColor = android.graphics.Color.BLACK
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxSteps.toFloat()

        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun generateTestData(
        labels: MutableList<String>,
        stackedEntries: MutableList<BarEntry>
    ): Int {
        val calendar = Calendar.getInstance()
        var maxGeneratedSteps = 0

        for (i in 0..6) {
            val testDate = calendar.clone() as Calendar
            testDate.add(Calendar.DAY_OF_YEAR, -(6 - i))
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(testDate.time)
            labels.add(dateStr)

            val randomSteps = Random.nextInt(1000, 15001).toFloat()
            maxGeneratedSteps = max(maxGeneratedSteps, randomSteps.toInt())
            val goal = dailyGoal.toFloat()

            if (randomSteps > goal) {
                stackedEntries.add(BarEntry((6 - i).toFloat(), floatArrayOf(goal, randomSteps - goal)))
            } else {
                stackedEntries.add(BarEntry((6 - i).toFloat(), floatArrayOf(randomSteps, 0f)))
            }
        }

        return maxGeneratedSteps
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
}
