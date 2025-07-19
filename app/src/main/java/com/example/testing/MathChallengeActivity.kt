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

class MathChallengeActivity : AppCompatActivity() {
    private lateinit var problemText: TextView
    private lateinit var problemNumberText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var newProblemButton: Button
    private var currentAnswer: Int = 0
    private var problemsSolved: Int = 0
    private val requiredProblems = 3 // User must solve 3 problems to access time limits
    private var targetPackageName: String = ""
    private var targetAppName: String = ""
    private var isTimeLimitAccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_challenge)

        problemText = findViewById(R.id.problemText)
        problemNumberText = findViewById(R.id.problemNumberText)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        newProblemButton = findViewById(R.id.newProblemButton)
        
        // Get target app info from intent
        targetPackageName = intent.getStringExtra("package_name") ?: ""
        targetAppName = intent.getStringExtra("app_name") ?: "this app"
        isTimeLimitAccess = intent.getBooleanExtra("is_time_limit_access", false)

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
                // Check if this is for time limit access or app time increase
                if (isTimeLimitAccess) {
                    // Allow access to time limit activity
                    allowTimeLimitAccess()
                } else {
                    // Increase time limit for the specific app
                    increaseTimeLimitForApp()
                }
            } else {
                generateNewProblem()
            }
        } else {
            Toast.makeText(this, "Incorrect! Try again", Toast.LENGTH_SHORT).show()
            answerInput.text.clear()
            answerInput.requestFocus()
        }
    }
    
    // This method is no longer used - app time increases are handled by AppTimeIncreaseMathActivity
    private fun increaseTimeLimitForApp() {
        // This should not be called anymore
        Toast.makeText(this, "Error: This functionality has been moved", Toast.LENGTH_SHORT).show()
        goToHome()
    }
    
    private fun allowTimeLimitAccess() {
        Toast.makeText(this, "Math challenge completed! Accessing time limits...", Toast.LENGTH_LONG).show()
        
        // Start time limit activity
        val intent = Intent(this, TimeLimitActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun goToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(homeIntent)
        finish()
    }
} 