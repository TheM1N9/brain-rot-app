package com.example.testing

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Request notification permission first (for Android 13+)
            requestNotificationPermission()
            
            // Request overlay permission for blocking screen
            requestOverlayPermission()

            setupUI()
            startBlockerServiceIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update UI when returning from other activities
        setupUI()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features may not work properly.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        try {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
        val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()

        val accessButton = findViewById<MaterialButton>(R.id.accessButton)
        val selectAppsButton = findViewById<MaterialButton>(R.id.selectAppsButton)
        val setTimeLimitsButton = findViewById<MaterialButton>(R.id.setTimeLimitsButton)
            val testButton = findViewById<MaterialButton>(R.id.testButton)

            // Show/hide access button based on permission
        if (hasUsageStatsPermission(this)) {
            accessButton.visibility = View.GONE
        } else {
            accessButton.visibility = View.VISIBLE
            accessButton.setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                Toast.makeText(this, "Please grant usage access in settings", Toast.LENGTH_SHORT).show()
            }
        }

            // Update button states based on app state
            if (hasSelectedApps) {
                selectAppsButton.text = "Change Selected Apps"
                setTimeLimitsButton.isEnabled = true
            } else {
                selectAppsButton.text = "Select Apps to Block"
                setTimeLimitsButton.isEnabled = false
            }

            if (hasTimeLimits) {
                setTimeLimitsButton.text = "Update Time Limits"
            } else {
                setTimeLimitsButton.text = "Set Time Limits"
            }

        selectAppsButton.setOnClickListener {
            if (hasUsageStatsPermission(this@MainActivity)) {
                startActivity(Intent(this@MainActivity, AppSelectionActivity::class.java))
            } else {
                Toast.makeText(this, "Please grant usage access first", Toast.LENGTH_SHORT).show()
            }
        }

        setTimeLimitsButton.setOnClickListener {
            if (hasSelectedApps) {
                startActivity(Intent(this@MainActivity, TimeLimitActivity::class.java))
            } else {
                Toast.makeText(this, "Please select apps first", Toast.LENGTH_SHORT).show()
                }
            }

            // Test button for debugging home redirection
            testButton.setOnClickListener {
                if (hasUsageStatsPermission(this) && hasSelectedApps && hasTimeLimits) {
                    // Enable test mode
                    prefs.edit().putBoolean("test_mode", true).apply()
                    Toast.makeText(this, "Test mode enabled. Service will force home redirection on next check.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Please complete setup first (permissions, apps, limits)", Toast.LENGTH_SHORT).show()
                }
            }

            // Update status text
            val statusTextView = findViewById<TextView>(R.id.statusText)
            when {
                !hasUsageStatsPermission(this) -> {
                    statusTextView.text = "❌ Usage access permission required\nTap 'Grant Usage Access' to continue"
                }
                !hasSelectedApps -> {
                    statusTextView.text = "📱 No apps selected for blocking\nTap 'Select Apps to Block' to get started"
                }
                !hasTimeLimits -> {
                    statusTextView.text = "⏰ Time limits not configured\nTap 'Set Time Limits' to configure daily limits"
                }
                else -> {
                    val serviceRunning = isServiceRunning()
                    val status = if (serviceRunning) "🟢 Active and monitoring" else "🔴 Service not running"
                    statusTextView.text = "$status\nYour digital wellness plan is ready to go!"
                    android.util.Log.d("MainActivity", "Service running: $serviceRunning")
                    
                    // Show current usage for debugging
                    showCurrentUsage()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBlockerServiceIfNeeded() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
            val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()

            // Debug logging
            android.util.Log.d("MainActivity", "hasUsageStatsPermission: ${hasUsageStatsPermission(this)}")
            android.util.Log.d("MainActivity", "hasSelectedApps: $hasSelectedApps")
            android.util.Log.d("MainActivity", "hasTimeLimits: $hasTimeLimits")

            if (hasUsageStatsPermission(this) && hasSelectedApps && hasTimeLimits) {
                // Add a small delay to ensure proper user interaction
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, BlockerService::class.java))
                        Toast.makeText(this@MainActivity, "Monitoring service started", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to start service: ${e.message}")
                        Toast.makeText(this@MainActivity, "Failed to start monitoring service: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }, 500) // 500ms delay
            } else {
                // Stop service if conditions are not met
                try {
                    stopService(Intent(this, BlockerService::class.java))
                } catch (e: Exception) {
                    // Ignore errors when stopping service
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error managing service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission needed for blocking screen. Please grant it in settings.", Toast.LENGTH_LONG).show()
                
                // Show a dialog explaining why we need overlay permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("To show the blocking screen over other apps, we need overlay permission. This allows us to display a blocking message directly over the app you're trying to use.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                    }
                    .setNegativeButton("Later") { _, _ ->
                        Toast.makeText(this, "You can grant overlay permission later in settings", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (android.provider.Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Overlay permission granted! Blocking screen will now work properly.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Overlay permission denied. Blocking screen may not work over other apps.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showCurrentUsage() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val blocked = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
            val timeLimits = prefs.getString("time_limits", null)
                ?.split("|")
                ?.mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
                }?.toMap() ?: emptyMap()

            val usageInfo = blocked.mapNotNull { pkg ->
                val used = UsageUtils.getAppUsageMinutes(this, pkg)
                val limit = timeLimits[pkg] ?: 0
                if (limit > 0) "$pkg: $used/$limit min" else null
            }

            if (usageInfo.isNotEmpty()) {
                android.util.Log.d("MainActivity", "Current usage: ${usageInfo.joinToString(", ")}")
                // Show usage in toast for debugging
                Toast.makeText(this, "Usage: ${usageInfo.joinToString(", ")}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isServiceRunning(): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use the newer API for Android 8.0+
                val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
                runningServices.any { service ->
                    BlockerService::class.java.name == service.service.className
                }
            } else {
                // Fallback for older versions
                val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
                runningServices.any { service ->
                    BlockerService::class.java.name == service.service.className
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking service status: ${e.message}")
            false
        }
    }
}
