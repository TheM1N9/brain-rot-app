package com.example.testing

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalUsageText: TextView
    private lateinit var periodText: TextView
    
    private var currentPeriod = "today"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        
        initializeViews()
        setupTabLayout()
        loadAnalytics()
    }
    
    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.analyticsRecyclerView)
        totalUsageText = findViewById(R.id.totalUsageText)
        periodText = findViewById(R.id.periodText)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Today"))
        tabLayout.addTab(tabLayout.newTab().setText("Week"))
        tabLayout.addTab(tabLayout.newTab().setText("Month"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentPeriod = "today"
                    1 -> currentPeriod = "week"
                    2 -> currentPeriod = "month"
                }
                loadAnalytics()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadAnalytics() {
        val selectedApps = getSelectedApps()
        val analyticsData = getAnalyticsData(currentPeriod)
        
        // Update total usage
        val totalMinutes = analyticsData.values.sum()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        totalUsageText.text = "${hours}h ${minutes}m"
        
        // Update period text
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        when (currentPeriod) {
            "today" -> periodText.text = "Today (${dateFormat.format(Date())})"
            "week" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val weekStart = dateFormat.format(calendar.time)
                val weekEnd = dateFormat.format(Date())
                periodText.text = "This Week ($weekStart - $weekEnd)"
            }
            "month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val monthStart = dateFormat.format(calendar.time)
                val monthEnd = dateFormat.format(Date())
                periodText.text = "This Month ($monthStart - $monthEnd)"
            }
        }
        
        // Create adapter with app usage data
        val appUsageList = selectedApps.mapNotNull { packageName ->
            val appName = getAppName(packageName)
            val usageMinutes = analyticsData[packageName] ?: 0
            val limitMinutes = getTimeLimit(packageName)
            
            AppUsageData(
                packageName = packageName,
                appName = appName,
                usageMinutes = usageMinutes,
                limitMinutes = limitMinutes
            )
        }.sortedByDescending { it.usageMinutes }
        
        val adapter = AnalyticsAdapter(appUsageList)
        recyclerView.adapter = adapter
    }
    
    private fun getSelectedApps(): Set<String> {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
    }
    
    private fun getAnalyticsData(period: String): Map<String, Int> {
        return when (period) {
            "today" -> UsageUtils.getUsage(this)
            "week" -> getWeeklyUsage()
            "month" -> getMonthlyUsage()
            else -> emptyMap()
        }
    }
    
    private fun getWeeklyUsage(): Map<String, Int> {
        // For now, return today's usage. In a real app, you'd store historical data
        return UsageUtils.getUsage(this)
    }
    
    private fun getMonthlyUsage(): Map<String, Int> {
        // For now, return today's usage. In a real app, you'd store historical data
        return UsageUtils.getUsage(this)
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun getTimeLimit(packageName: String): Int {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val timeLimits = prefs.getString("time_limits", null)
            ?.split("|")
            ?.mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }?.toMap() ?: emptyMap()
        
        return timeLimits[packageName] ?: 0
    }
    
    data class AppUsageData(
        val packageName: String,
        val appName: String,
        val usageMinutes: Int,
        val limitMinutes: Int
    )
} 