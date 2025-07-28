package com.utnay.stepupquest

import android.content.Context
import android.content.SharedPreferences

class StatsDataHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("step_stats", Context.MODE_PRIVATE)

    fun saveDailySteps(date: String, steps: Int) {
        sharedPreferences.edit()
            .putInt("steps_$date", steps)
            .apply()
    }

    fun getDailySteps(date: String): Int {
        return sharedPreferences.getInt("steps_$date", 0)
    }

    fun getAllDailySteps(): Map<String, Int> {
        val allData = mutableMapOf<String, Int>()
        val keys = sharedPreferences.all.keys
        for (key in keys) {
            if (key.startsWith("steps_")) {
                val date = key.substring(6) // Remove "steps_" prefix
                allData[date] = sharedPreferences.getInt(key, 0)
            }
        }
        return allData
    }
}