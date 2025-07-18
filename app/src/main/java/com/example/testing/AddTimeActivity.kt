package com.example.testing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class AddTimeActivity : AppCompatActivity() {
    private lateinit var problemText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var newProblemButton: Button
    private lateinit var appNameText: TextView
    private lateinit var add5Button: Button
    private lateinit var add10Button: Button
    private lateinit var add15Button: Button
    private var currentAnswer: Int = 0
    private var problemsSolved: Int = 0
    private val requiredProblems = 2 // User must solve 2 problems to add time
    private var blockedAppPackage: String = ""
    private var timeToAdd: Int = 5 // Add 5 minutes by default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_time)

        blockedAppPackage = intent.getStringExtra("blocked_app_package") ?: ""
        val appName = intent.getStringExtra("app_name") ?: "App"

        problemText = findViewById(R.id.problemText)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        newProblemButton = findViewById(R.id.newProblemButton)
        appNameText = findViewById(R.id.appNameText)
        add5Button = findViewById(R.id.add5Button)
        add10Button = findViewById(R.id.add10Button)
        add15Button = findViewById(R.id.add15Button)

        appNameText.text = "Add time to: $appName"

        // Hide add time buttons initially
        add5Button.visibility = View.GONE
        add10Button.visibility = View.GONE
        add15Button.visibility = View.GONE

        generateNewProblem()

        submitButton.setOnClickListener {
            checkAnswer()
        }

        newProblemButton.setOnClickListener {
            generateNewProblem()
        }

        add5Button.setOnClickListener { addTimeAndRedirect(5) }
        add10Button.setOnClickListener { addTimeAndRedirect(10) }
        add15Button.setOnClickListener { addTimeAndRedirect(15) }
    }

    private fun generateNewProblem() {
        val num1 = Random.nextInt(1, 50)
        val num2 = Random.nextInt(1, 50)
        val operation = Random.nextInt(0, 4) // 0: +, 1: -, 2: *, 3: /

        val problem: String
        val answer: Int
        
        when (operation) {
            0 -> {
                problem = "$num1 + $num2 = ?"
                answer = num1 + num2
            }
            1 -> {
                problem = "$num1 - $num2 = ?"
                answer = num1 - num2
            }
            2 -> {
                problem = "$num1 × $num2 = ?"
                answer = num1 * num2
            }
            3 -> {
                val result = num1 * num2
                problem = "$result ÷ $num2 = ?"
                answer = num1
            }
            else -> {
                problem = "$num1 + $num2 = ?"
                answer = num1 + num2
            }
        }

        problemText.text = "Problem ${problemsSolved + 1}/$requiredProblems: $problem"
        currentAnswer = answer
        answerInput.text.clear()
        answerInput.requestFocus()
    }

    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().toIntOrNull()
        
        if (userAnswer == null) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        if (userAnswer == currentAnswer) {
            problemsSolved++
            Toast.makeText(this, "Correct! ${requiredProblems - problemsSolved} more to go", Toast.LENGTH_SHORT).show()
            
            if (problemsSolved >= requiredProblems) {
                // Show add time buttons
                problemText.text = "How much time do you want to add?"
                answerInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                newProblemButton.visibility = View.GONE
                add5Button.visibility = View.VISIBLE
                add10Button.visibility = View.VISIBLE
                add15Button.visibility = View.VISIBLE
            } else {
                generateNewProblem()
            }
        } else {
            Toast.makeText(this, "Incorrect! Try again", Toast.LENGTH_SHORT).show()
            answerInput.text.clear()
            answerInput.requestFocus()
        }
    }

    private fun addTimeAndRedirect(minutes: Int) {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val timeLimitsStr = prefs.getString("time_limits", null)
        val timeLimits = mutableMapOf<String, Int>()
        
        timeLimitsStr?.split("|")?.forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) timeLimits[parts[0]] = parts[1].toIntOrNull() ?: 0
        }
        
        // Add time to the blocked app
        val currentLimit = timeLimits[blockedAppPackage] ?: 0
        timeLimits[blockedAppPackage] = currentLimit + minutes
        
        // Save updated time limits
        val newTimeLimitsStr = timeLimits.entries.joinToString("|") { "${it.key},${it.value}" }
        prefs.edit().putString("time_limits", newTimeLimitsStr).apply()

        Toast.makeText(this, "Time limit extended by $minutes minutes!", Toast.LENGTH_LONG).show()
        redirectToApp(blockedAppPackage)
    }

    private fun redirectToApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // If we can't launch the app, just finish this activity
        }
        finish()
    }
} 