package com.example.testing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class OverlayBlockingService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countdownSeconds = 8
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverlayBlockingService", "Starting overlay blocking service")
        
        // Show the overlay immediately
        showBlockingOverlay()
        
        // Auto-hide after countdown
        handler.postDelayed({
            hideBlockingOverlay()
        }, (countdownSeconds * 1000).toLong())
        
        return START_NOT_STICKY
    }
    
    private fun showBlockingOverlay() {
        try {
            if (overlayView != null) {
                android.util.Log.d("OverlayBlockingService", "Overlay already showing")
                return
            }
            
            // Create the overlay view
            overlayView = createOverlayView()
            
            // Set up window parameters for overlay
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
            }
            
            // Add the overlay to the window
            windowManager.addView(overlayView, params)
            
            android.util.Log.d("OverlayBlockingService", "Overlay blocking screen shown over target app")
            
            // Start countdown
            startCountdown()
            
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockingService", "Failed to show overlay: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createOverlayView(): View {
        // Create a full-screen blocking layout
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setBackgroundColor(0xFF6366F1.toInt()) // Updated primary color
        layout.setPadding(64, 64, 64, 64)
        
        // Title
        val title = TextView(this)
        title.text = "🚫 App Blocked!"
        title.textSize = 36f
        title.setTextColor(0xFFFFFFFF.toInt())
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(0, 0, 0, 32)
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(title)
        
        // Message
        val message = TextView(this)
        message.text = "You've reached your daily time limit.\n\nTaking a break is good for you! 😊\n\nTime to focus on what really matters."
        message.textSize = 18f
        message.setTextColor(0xFFFFFFFF.toInt())
        message.gravity = android.view.Gravity.CENTER
        message.setPadding(0, 0, 0, 32)
        message.setLineSpacing(8f, 1.2f)
        layout.addView(message)
        
        // Countdown text
        val countdownTextView = TextView(this)
        countdownTextView.text = "Redirecting in $countdownSeconds seconds..."
        countdownTextView.textSize = 20f
        countdownTextView.setTextColor(0xFFFFFFFF.toInt())
        countdownTextView.gravity = android.view.Gravity.CENTER
        countdownTextView.setPadding(0, 0, 0, 48)
        countdownTextView.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(countdownTextView)
        
        // Home button
        val homeButton = Button(this)
        homeButton.text = "🏠 Go to Home"
        homeButton.textSize = 18f
        homeButton.setTextColor(0xFF6366F1.toInt())
        homeButton.setBackgroundColor(0xFFFFFFFF.toInt())
        homeButton.setPadding(32, 16, 32, 16)
        homeButton.setOnClickListener {
            goToHome()
        }
        layout.addView(homeButton)
        
        // Store reference to countdown text view for updates
        layout.tag = countdownTextView
        
        return layout
    }
    
    private fun startCountdown() {
        val countdownRunnable = object : Runnable {
            override fun run() {
                countdownSeconds--
                val countdownTextView = overlayView?.tag as? TextView
                countdownTextView?.text = "Redirecting in $countdownSeconds seconds..."
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                } else {
                    goToHome()
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(homeIntent)
            hideBlockingOverlay()
        } catch (e: Exception) {
            Toast.makeText(this, "Error going to home: ${e.message}", Toast.LENGTH_SHORT).show()
            hideBlockingOverlay()
        }
    }
    
    private fun hideBlockingOverlay() {
        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView)
                overlayView = null
                android.util.Log.d("OverlayBlockingService", "Overlay blocking screen hidden")
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockingService", "Failed to hide overlay: ${e.message}")
        }
        
        // Stop the service
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        hideBlockingOverlay()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 