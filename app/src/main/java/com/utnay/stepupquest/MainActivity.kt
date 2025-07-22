package com.utnay.stepupquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var stepCount = 0
    private var dailyGoal = 10000

    // Declaración de vistas
    private lateinit var textViewSteps: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMeta: Button
    private lateinit var btnEst: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vinculación de vistas con sus IDs correctos
        textViewSteps = findViewById(R.id.textViewSteps)
        textViewGoal = findViewById(R.id.textViewGoal)
        progressBar = findViewById(R.id.progressBar)
        btnMeta = findViewById(R.id.btnMeta)
        btnEst = findViewById(R.id.btnEst)

        // Inicializar UI
        updateStepCount(stepCount)
        updateGoal(dailyGoal)

        // Botón: Configurar Meta
        btnMeta.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("currentGoal", dailyGoal)
            startActivityForResult(intent, REQUEST_CODE_SET_GOAL)
        }

        /* Botón: Ver Estadísticas (pendiente de implementar)
        btnEst.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SET_GOAL && resultCode == RESULT_OK) {
            data?.getIntExtra("newGoal", 10000)?.let { newGoal ->
                updateGoal(newGoal)
            }
        }
    }

    private fun updateStepCount(steps: Int) {
        stepCount = steps
        textViewSteps.text = "$steps pasos"
        updateProgressBar()
    }

    private fun updateGoal(goal: Int) {
        dailyGoal = goal
        textViewGoal.text = "META: $goal pasos"
        updateProgressBar()
    }

    private fun updateProgressBar() {
        val percentage = if (dailyGoal == 0) 0 else (stepCount * 100) / dailyGoal
        progressBar.progress = percentage
    }

    companion object {
        private const val REQUEST_CODE_SET_GOAL = 1
    }
}