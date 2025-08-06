package com.utnay.stepupquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class GoalActivity : AppCompatActivity() {

    private lateinit var editTextGoal: EditText
    private lateinit var buttonSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        editTextGoal = findViewById(R.id.editTextGoal)
        buttonSave = findViewById(R.id.buttonSaveGoal)

        // Obtener la meta actual si fue pasada desde MainActivity
        val currentGoal = intent.getIntExtra("currentGoal", 10000)
        editTextGoal.setText(currentGoal.toString())

        // Al hacer clic en "Guardar"
        buttonSave.setOnClickListener {
            val newGoalString = editTextGoal.text.toString()

            // Validar que no esté vacío
            if (newGoalString.isNotEmpty()) {
                val newGoal = newGoalString.toInt()

                // Devolver la nueva meta a MainActivity
                val resultIntent = Intent()
                resultIntent.putExtra("newGoal", newGoal)
                setResult(RESULT_OK, resultIntent)
                finish() // Cierra esta pantalla y regresa
            }
        }
    }
}