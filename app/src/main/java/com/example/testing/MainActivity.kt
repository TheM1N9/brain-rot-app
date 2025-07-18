package com.example.testing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var selectAppsButton: MaterialButton
    private lateinit var setLimitsButton: MaterialButton
    private lateinit var viewAnalyticsButton: MaterialButton
    
    private lateinit var todayTotalText: TextView
    private lateinit var blockedAppsText: TextView
    private lateinit var limitsReachedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
        updateUI()
        
        // Start the blocker service if permission is granted
        if (hasUsageStatsPermission(this)) {
            startBlockerService()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUsageStats()
    }
    
    private fun initializeViews() {
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        selectAppsButton = findViewById(R.id.selectAppsButton)
        setLimitsButton = findViewById(R.id.setLimitsButton)
        viewAnalyticsButton = findViewById(R.id.viewAnalyticsButton)
        
        todayTotalText = findViewById(R.id.todayTotalText)
        blockedAppsText = findViewById(R.id.blockedAppsText)
        limitsReachedText = findViewById(R.id.limitsReachedText)
    }
    
    private fun setupClickListeners() {
        grantPermissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
        
        selectAppsButton.setOnClickListener {
            val intent = Intent(this, AppSelectionActivity::class.java)
            startActivity(intent)
        }
        
        setLimitsButton.setOnClickListener {
            val intent = Intent(this, MathChallengeActivity::class.java)
            startActivity(intent)
        }
        
        viewAnalyticsButton.setOnClickListener {
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun updateUI() {
        if (hasUsageStatsPermission(this)) {
            grantPermissionButton.visibility = View.GONE
            selectAppsButton.visibility = View.VISIBLE
            setLimitsButton.visibility = View.VISIBLE
            viewAnalyticsButton.visibility = View.VISIBLE
        } else {
            grantPermissionButton.visibility = View.VISIBLE
            selectAppsButton.visibility = View.GONE
            setLimitsButton.visibility = View.GONE
            viewAnalyticsButton.visibility = View.GONE
        }
    }
    
    private fun updateUsageStats() {
        if (!hasUsageStatsPermission(this)) return
        
        // Reset daily usage if needed
        UsageUtils.resetIfNeeded(this)
        
        // Get today's usage
        val todayUsage = UsageUtils.getUsage(this)
        val selectedApps = getSelectedApps()
        
        // Calculate total usage
        val totalMinutes = todayUsage.values.sum()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        todayTotalText.text = "${hours}h ${minutes}m"
        
        // Show number of blocked apps
        blockedAppsText.text = selectedApps.size.toString()
        
        // Count how many times limits were reached today
        val limitsReached = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getInt("limits_reached_today", 0)
        limitsReachedText.text = limitsReached.toString()
    }
    
    private fun getSelectedApps(): Set<String> {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
    }
    
    private fun startBlockerService() {
        val intent = Intent(this, BlockerService::class.java)
        startForegroundService(intent)
    }
    
    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
