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

class TimeIncreaseActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var countdownSeconds = 10
    private var appName: String = ""
    private var packageName: String = ""
    private var isActivityActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("TimeIncreaseActivity", "onCreate started - Intent: ${intent?.action}")
        android.util.Log.d("TimeIncreaseActivity", "onCreate - Intent extras: ${intent?.extras}")
        
        try {
            android.util.Log.d("TimeIncreaseActivity", "onCreate started")
            
            // Get app info from intent
            appName = intent.getStringExtra("app_name") ?: "This app"
            packageName = intent.getStringExtra("package_name") ?: ""
            
            android.util.Log.d("TimeIncreaseActivity", "App: $appName, Package: $packageName")
            
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
            
            setContentView(R.layout.activity_time_increase)
            android.util.Log.d("TimeIncreaseActivity", "Layout set successfully")
            
            // Initialize views
            countdownTextView = findViewById(R.id.countdownText)
            android.util.Log.d("TimeIncreaseActivity", "Views initialized")
            
            // Set app name in the question
            findViewById<TextView>(R.id.questionText).text = "Do you want to increase the time limit for $appName?"
            
            // Set up buttons
            findViewById<Button>(R.id.yesButton).setOnClickListener {
                onYesClicked()
            }
            
            findViewById<Button>(R.id.noButton).setOnClickListener {
                onNoClicked()
            }
            
            android.util.Log.d("TimeIncreaseActivity", "Buttons set up successfully")
            
            // Start countdown
            startCountdown()
            
            // Automatically redirect to home after countdown
            handler.postDelayed({
                if (isActivityActive) {
                    goToHome()
                }
            }, (countdownSeconds * 1000).toLong())
            
            android.util.Log.d("TimeIncreaseActivity", "onCreate completed successfully")
            
            // Show a toast to confirm the activity is working
            Toast.makeText(this, "Time increase screen shown for $appName", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("TimeIncreaseActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            
            // Emergency fallback: go to home
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("TimeIncreaseActivity", "Failed to go to home: ${e2.message}")
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
    
    private fun onYesClicked() {
        android.util.Log.d("TimeIncreaseActivity", "Yes button clicked for $appName")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        try {
            // Start app time increase math challenge
            val intent = Intent(this, AppTimeIncreaseMathActivity::class.java)
            intent.putExtra("package_name", packageName)
            intent.putExtra("app_name", appName)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TimeIncreaseActivity", "Failed to start AppTimeIncreaseMathActivity: ${e.message}")
            goToHome()
        }
    }
    
    private fun onNoClicked() {
        android.util.Log.d("TimeIncreaseActivity", "No button clicked for $appName")
        
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
        android.util.Log.d("TimeIncreaseActivity", "onResume called")
        
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
        android.util.Log.d("TimeIncreaseActivity", "onPause called")
    }
    
    override fun onDestroy() {
        isActivityActive = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 