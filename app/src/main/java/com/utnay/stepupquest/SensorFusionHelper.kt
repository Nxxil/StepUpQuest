package com.utnay.stepupquest

import android.hardware.SensorEvent
import kotlin.math.abs
import kotlin.math.sqrt

class SensorFusionHelper {

    private var lastAccelValues = FloatArray(3)
    private var lastGyroValues = FloatArray(3)
    private var lastTimestamp: Long = 0

    companion object {
        private const val ACCEL_THRESHOLD = 2.0f
        private const val GYRO_THRESHOLD = 1.0f
        private const val MIN_TIME_BETWEEN_STEPS = 100L // ms
    }

    fun isStepDetected(accelEvent: SensorEvent?, gyroEvent: SensorEvent?): Boolean {
        if (accelEvent == null) return false

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimestamp < MIN_TIME_BETWEEN_STEPS) {
            return false
        }

        // Verificar acelerómetro
        val accelX = accelEvent.values[0]
        val accelY = accelEvent.values[1]
        val accelZ = accelEvent.values[2]
        val currentAccel = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)

        val accelDiff = abs(currentAccel - sqrt(
            lastAccelValues[0] * lastAccelValues[0] +
                    lastAccelValues[1] * lastAccelValues[1] +
                    lastAccelValues[2] * lastAccelValues[2]
        ))

        // Guardar valores actuales
        lastAccelValues[0] = accelX
        lastAccelValues[1] = accelY
        lastAccelValues[2] = accelZ

        if (gyroEvent != null) {
            // Verificar giroscopio
            val gyroDiff = abs(gyroEvent.values[0] - lastGyroValues[0]) +
                    abs(gyroEvent.values[1] - lastGyroValues[1]) +
                    abs(gyroEvent.values[2] - lastGyroValues[2])

            // Guardar valores actuales del giroscopio
            lastGyroValues[0] = gyroEvent.values[0]
            lastGyroValues[1] = gyroEvent.values[1]
            lastGyroValues[2] = gyroEvent.values[2]

            lastTimestamp = currentTime

            // Ambos sensores deben detectar movimiento significativo
            return accelDiff > ACCEL_THRESHOLD && gyroDiff > GYRO_THRESHOLD
        } else {
            // Solo acelerómetro
            lastTimestamp = currentTime
            return accelDiff > ACCEL_THRESHOLD
        }
    }
}