package com.example.testing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class AppTimeIncreaseMathActivity : AppCompatActivity() {
    private lateinit var problemText: TextView
    private lateinit var problemNumberText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var newProblemButton: Button
    private var currentAnswer: Int = 0
    private var problemsSolved: Int = 0
    private val requiredProblems = 2 // User must solve 2 problems to increase app time
    private var targetPackageName: String = ""
    private var targetAppName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_time_increase_math)

        problemText = findViewById(R.id.problemText)
        problemNumberText = findViewById(R.id.problemNumberText)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        newProblemButton = findViewById(R.id.newProblemButton)
        
        // Get target app info from intent
        targetPackageName = intent.getStringExtra("package_name") ?: ""
        targetAppName = intent.getStringExtra("app_name") ?: "this app"

        generateNewProblem()

        submitButton.setOnClickListener {
            checkAnswer()
        }

        newProblemButton.setOnClickListener {
            generateNewProblem()
        }
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

        problemNumberText.text = "Problem ${problemsSolved + 1} of $requiredProblems"
        problemText.text = problem
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
                // Show time increase options
                showTimeIncreaseOptions()
            } else {
                generateNewProblem()
            }
        } else {
            Toast.makeText(this, "Incorrect! Try again", Toast.LENGTH_SHORT).show()
            answerInput.text.clear()
            answerInput.requestFocus()
        }
    }
    
    private fun showTimeIncreaseOptions() {
        // Hide math challenge UI
        findViewById<TextView>(R.id.titleText).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.problemNumberText).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.problemText).visibility = android.view.View.GONE
        findViewById<EditText>(R.id.answerInput).visibility = android.view.View.GONE
        findViewById<Button>(R.id.submitButton).visibility = android.view.View.GONE
        findViewById<Button>(R.id.newProblemButton).visibility = android.view.View.GONE
        
        // Show time increase options
        findViewById<TextView>(R.id.timeIncreaseTitle).visibility = android.view.View.VISIBLE
        findViewById<TextView>(R.id.timeIncreaseMessage).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.fiveMinButton).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.tenMinButton).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.fifteenMinButton).visibility = android.view.View.VISIBLE
        
        // Set up button listeners
        findViewById<Button>(R.id.fiveMinButton).setOnClickListener {
            increaseTimeLimit(5)
        }
        
        findViewById<Button>(R.id.tenMinButton).setOnClickListener {
            increaseTimeLimit(10)
        }
        
        findViewById<Button>(R.id.fifteenMinButton).setOnClickListener {
            increaseTimeLimit(15)
        }
    }
    
    private fun increaseTimeLimit(minutes: Int) {
        if (targetPackageName.isNotEmpty()) {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val timeLimits = prefs.getString("time_limits", null)
                ?.split("|")
                ?.mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
                }?.toMap()?.toMutableMap() ?: mutableMapOf()
            
            // Increase time limit by specified minutes
            val currentLimit = timeLimits[targetPackageName] ?: 0
            val newLimit = currentLimit + minutes
            timeLimits[targetPackageName] = newLimit
            
            // Save updated time limits
            val timeLimitsString = timeLimits.map { "${it.key},${it.value}" }.joinToString("|")
            prefs.edit().putString("time_limits", timeLimitsString).apply()
            
            Toast.makeText(this, "Time limit for $targetAppName increased by $minutes minutes!", Toast.LENGTH_LONG).show()
        }
        
        // Redirect to the target app instead of home screen
        redirectToTargetApp()
    }
    
    private fun redirectToTargetApp() {
        try {
            if (targetPackageName.isNotEmpty()) {
                val intent = packageManager.getLaunchIntentForPackage(targetPackageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    android.util.Log.d("AppTimeIncreaseMathActivity", "Redirected to target app: $targetPackageName")
                } else {
                    android.util.Log.e("AppTimeIncreaseMathActivity", "Could not get launch intent for: $targetPackageName")
                    goToHome()
                }
            } else {
                android.util.Log.e("AppTimeIncreaseMathActivity", "No target package name available")
                goToHome()
            }
        } catch (e: Exception) {
            android.util.Log.e("AppTimeIncreaseMathActivity", "Failed to redirect to target app: ${e.message}")
            goToHome()
        }
        finish()
    }
    
    private fun goToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(homeIntent)
    }
} 