package com.example.testing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimeLimitBlockingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var countdownSeconds = 8
    private var isActivityActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate started - Intent: ${intent?.action}")
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate - Intent extras: ${intent?.extras}")
        
        try {
            android.util.Log.d("TimeLimitBlockingActivity", "onCreate started")
            
            // Make the activity full screen and always on top
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        
            // Hide system UI
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            
            setContentView(R.layout.activity_time_limit_blocking)
            android.util.Log.d("TimeLimitBlockingActivity", "Layout set successfully")
            
            // Initialize views
            countdownTextView = findViewById(R.id.countdownText)
            android.util.Log.d("TimeLimitBlockingActivity", "Views initialized")
            
            // Set up buttons
            findViewById<Button>(R.id.continueButton).setOnClickListener {
                onContinueClicked()
            }
            
            findViewById<Button>(R.id.cancelButton).setOnClickListener {
                onCancelClicked()
            }
            
            android.util.Log.d("TimeLimitBlockingActivity", "Buttons set up successfully")
            
            // Start countdown
            startCountdown()
            
            // Automatically redirect to home after countdown
            handler.postDelayed({
                if (isActivityActive) {
                    goToHome()
                }
            }, (countdownSeconds * 1000).toLong())
            
            android.util.Log.d("TimeLimitBlockingActivity", "onCreate completed successfully")
            
            // Show a toast to confirm the activity is working
            Toast.makeText(this, "Time limit blocking screen shown", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            
            // Emergency fallback: go to home
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("TimeLimitBlockingActivity", "Failed to go to home: ${e2.message}")
                finish()
            }
        }
    }
    
    private fun startCountdown() {
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (!isActivityActive) return
                
                countdownSeconds--
                countdownTextView?.text = "Redirecting in $countdownSeconds seconds..."
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun onContinueClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Continue button clicked")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        try {
            // Start math challenge activity instead of directly going to time limit activity
            val intent = Intent(this, MathChallengeActivity::class.java)
            intent.putExtra("package_name", "time_limit_access")
            intent.putExtra("app_name", "Time Limit Access")
            intent.putExtra("is_time_limit_access", true)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Failed to start MathChallengeActivity: ${e.message}")
            goToHome()
        }
    }
    
    private fun onCancelClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Cancel button clicked")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        // Go to home screen
        goToHome()
    }
    
    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error going to home: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onBackPressed() {
        // Prevent back button from closing the screen
        Toast.makeText(this, "Please make a choice or wait for the timer.", Toast.LENGTH_SHORT).show()
    }
    
    override fun onResume() {
        super.onResume()
        isActivityActive = true
        android.util.Log.d("TimeLimitBlockingActivity", "onResume called")
        
        // Ensure we stay in foreground
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or 
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or 
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }
    
    override fun onPause() {
        super.onPause()
        isActivityActive = false
        android.util.Log.d("TimeLimitBlockingActivity", "onPause called")
    }
    
    override fun onDestroy() {
        isActivityActive = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 