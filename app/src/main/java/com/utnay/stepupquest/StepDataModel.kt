package com.utnay.stepupquest

data class StepData(
    val mobileSteps: Int,
    val wearSteps: Int,
    val totalSteps: Int,
    val timestamp: Long,
    val dailyGoal: Int
)