package com.utnay.stepupquest

data class StepData(
    val mobileSteps: Int,
    val wearSteps: Int,
    val totalSteps: Int,
    val timestamp: Long,
    val dailyGoal: Int,
    val percentage: Int = if (dailyGoal == 0) 0 else (totalSteps * 100) / dailyGoal
)