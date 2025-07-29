package com.utnay.stepupquest

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class DataStorageManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("step_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_DAILY_STEPS = "daily_steps"
        private const val KEY_STEP_HISTORY = "step_history"
        private const val DEFAULT_DAILY_GOAL = 10000
    }

    // Guardar meta diaria
    fun saveDailyGoal(goal: Int) {
        sharedPreferences.edit()
            .putInt(KEY_DAILY_GOAL, goal)
            .apply()
    }

    // Obtener meta diaria
    fun getDailyGoal(): Int {
        return sharedPreferences.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    }

    // Guardar pasos diarios
    fun saveDailySteps(steps: Int) {
        val today = getCurrentDate()
        sharedPreferences.edit()
            .putInt("${KEY_DAILY_STEPS}_$today", steps)
            .apply()
    }

    // Obtener pasos de hoy
    fun getTodaySteps(): Int {
        val today = getCurrentDate()
        return sharedPreferences.getInt("${KEY_DAILY_STEPS}_$today", 0)
    }

    // Guardar historial de pasos
    fun saveStepHistory(stepsMap: Map<String, Int>) {
        val json = gson.toJson(stepsMap)
        sharedPreferences.edit()
            .putString(KEY_STEP_HISTORY, json)
            .apply()
    }

    // Obtener historial de pasos
    fun getStepHistory(): Map<String, Int> {
        val json = sharedPreferences.getString(KEY_STEP_HISTORY, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableMapOf()
        }
    }

    // Agregar pasos de hoy al historial
    fun addToHistory(steps: Int) {
        val today = getCurrentDate()
        val history = getStepHistory().toMutableMap()
        history[today] = steps
        saveStepHistory(history)
    }

    // Obtener datos de la semana (últimos 7 días)
    fun getWeeklySteps(): Map<String, Int> {
        val calendar = Calendar.getInstance()
        val weeklyData = mutableMapOf<String, Int>()
        val history = getStepHistory()

        // Obtener últimos 7 días
        for (i in 0..6) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.time)
            weeklyData[date] = history[date] ?: 0
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return weeklyData
    }

    // Obtener datos de los últimos 30 días
    fun getMonthlySteps(): Map<String, Int> {
        val calendar = Calendar.getInstance()
        val monthlyData = mutableMapOf<String, Int>()
        val history = getStepHistory()

        // Obtener últimos 30 días
        for (i in 0..29) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.time)
            monthlyData[date] = history[date] ?: 0
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return monthlyData
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}