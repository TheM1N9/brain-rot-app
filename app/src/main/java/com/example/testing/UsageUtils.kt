package com.example.testing

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object UsageUtils {
    private const val PREFS_NAME = "blocked_apps"
    private const val USAGE_KEY = "usage_today"
    private const val LAST_RESET_KEY = "last_reset_date"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun resetIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = dateFormat.format(Date())
        val lastReset = prefs.getString(LAST_RESET_KEY, null)
        if (lastReset != today) {
            prefs.edit().remove(USAGE_KEY).putString(LAST_RESET_KEY, today).apply()
        }
    }

    fun getUsage(context: Context): MutableMap<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usageStr = prefs.getString(USAGE_KEY, "") ?: ""
        val map = mutableMapOf<String, Int>()
        if (usageStr.isNotEmpty()) {
            usageStr.split("|").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size == 2) map[parts[0]] = parts[1].toIntOrNull() ?: 0
            }
        }
        return map
    }

    fun setUsage(context: Context, usage: Map<String, Int>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = usage.entries.joinToString("|") { "${it.key},${it.value}" }
        prefs.edit().putString(USAGE_KEY, str).apply()
    }

    fun incrementUsage(context: Context, packageName: String, minutes: Int) {
        val usage = getUsage(context)
        val currentUsage = usage[packageName] ?: 0
        val newUsage = currentUsage + minutes
        usage[packageName] = newUsage
        setUsage(context, usage)
        
        android.util.Log.d("UsageUtils", "Incremented $packageName: $currentUsage -> $newUsage (+$minutes)")
    }

    fun getAppUsage(context: Context, packageName: String): Int {
        return getUsage(context)[packageName] ?: 0
    }

    fun incrementUsageSeconds(context: Context, packageName: String, seconds: Int) {
        // Store usage in seconds, but keep the interface in minutes for compatibility
        val usage = getUsageSeconds(context)
        val currentUsage = usage[packageName] ?: 0
        val newUsage = currentUsage + seconds
        usage[packageName] = newUsage
        setUsageSeconds(context, usage)
        android.util.Log.d("UsageUtils", "Incremented $packageName: $currentUsage -> $newUsage (+$seconds seconds)")
    }

    fun getAppUsageMinutes(context: Context, packageName: String): Int {
        // Return usage in minutes (rounded down)
        return (getUsageSeconds(context)[packageName] ?: 0) / 60
    }

    // Helper methods for seconds-based storage
    private fun getUsageSeconds(context: Context): MutableMap<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usageStr = prefs.getString(USAGE_KEY, "") ?: ""
        val map = mutableMapOf<String, Int>()
        if (usageStr.isNotEmpty()) {
            usageStr.split("|").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size == 2) map[parts[0]] = parts[1].toIntOrNull() ?: 0
            }
        }
        return map
    }

    private fun setUsageSeconds(context: Context, usage: Map<String, Int>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = usage.entries.joinToString("|") { "${it.key},${it.value}" }
        prefs.edit().putString(USAGE_KEY, str).apply()
    }
} 