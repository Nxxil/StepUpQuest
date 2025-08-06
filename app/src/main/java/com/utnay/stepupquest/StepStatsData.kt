package com.utnay.stepupquest

/**
 * Clase de datos que representa la información estadística completa a enviar a la TV.
 * Debe coincidir exactamente con la clase StepStatsData en la aplicación de TV.
 */
data class StepStatsData(
    /** Mapa de fecha (yyyy-MM-dd) a número de pasos para esa fecha */
    val dailySteps: Map<String, Int> = emptyMap(),

    /** Meta diaria de pasos configurada por el usuario */
    val dailyGoal: Int = 10000,

    /** Número de pasos registrados en el día actual */
    val currentSteps: Int = 0,

    /** Número total de pasos acumulados (puede ser semanal, mensual, etc., según tu lógica) */
    val totalSteps: Int = 0
)