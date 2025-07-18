package com.example.testing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.provider.Settings
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.widget.Toast
import android.util.Log

class BlockerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 2000L // Reduced to 2 seconds for more responsive blocking
    private var lastCheckedPackage: String? = null
    private var lastForegroundTimestamp: Long = 0L
    private var lastKnownForegroundApp: String? = null
    private var blockingInProgress = false // Flag to prevent multiple simultaneous blocking attempts

    override fun onCreate() {
        super.onCreate()
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            UsageUtils.resetIfNeeded(this@BlockerService)
            checkAndBlockApps()
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    private fun checkAndBlockApps() {
        if (!hasUsageStatsPermission(this)) {
            Toast.makeText(this, "Usage access permission required", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 // Check last 1 second
            
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    val packageName = event.packageName
                    checkAppLimit(packageName)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockerService", "Error checking apps: ${e.message}")
        }
    }
    
    private fun checkAppLimit(packageName: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val selectedApps = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        
        if (packageName in selectedApps) {
            val timeLimits = prefs.getString("time_limits", null)
                ?.split("|")
                ?.mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
                }?.toMap() ?: emptyMap()
            
            val limit = timeLimits[packageName] ?: 0
            val currentUsage = UsageUtils.getAppUsage(this, packageName)
            
            if (limit > 0 && currentUsage >= limit) {
                // Increment limits reached counter
                val currentCount = prefs.getInt("limits_reached_today", 0)
                prefs.edit().putInt("limits_reached_today", currentCount + 1).apply()
                
                // Launch AddTimeActivity
                val intent = Intent(this, AddTimeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("blocked_app_package", packageName)
                    putExtra("app_name", getAppName(packageName))
                }
                startActivity(intent)
            }
        }
    }

    private fun getForegroundAppPackageName(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - 10000 // Increased window to 10 seconds for better detection
            
            // Method 1: Try usage events first
            val events = usm.queryEvents(begin, end)
            var lastPackage: String? = null
            var lastTimestamp = 0L
            val event = UsageEvents.Event()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.timeStamp > lastTimestamp) {
                    lastPackage = event.packageName
                        lastTimestamp = event.timeStamp
                    }
                }
            }
            
            // Method 2: If no events found, try usage stats
            if (lastPackage == null) {
                val usageStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
                var mostRecent: String? = null
                var mostRecentTime = 0L
                
                for (stat in usageStats) {
                    if (stat.lastTimeUsed > mostRecentTime) {
                        mostRecent = stat.packageName
                        mostRecentTime = stat.lastTimeUsed
                    }
                }
                lastPackage = mostRecent
            }
            
            android.util.Log.d("BlockerService", "Detected foreground app: $lastPackage")
            lastPackage
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("BlockerService", "Error getting foreground app: ${e.message}")
            null
        }
    }

    private fun redirectToHome() {
        val currentApp = getForegroundAppPackageName() ?: return
        val appName = getAppName(currentApp)
        
        val intent = Intent(this, AddTimeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("blocked_app_package", currentApp)
        intent.putExtra("app_name", appName)
        startActivity(intent)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun tryKillBlockedApp(packageName: String) {
        try {
            android.util.Log.d("BlockerService", "Attempting to kill blocked app: $packageName")
            
            // Try to request the system to kill the app (limited effectiveness on modern Android)
            // Note: This method is deprecated and has limited effectiveness, but we'll try it anyway
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // This will only work if the app is in background, not if it's currently in foreground
            activityManager.killBackgroundProcesses(packageName)
            
            android.util.Log.d("BlockerService", "Requested system to kill background processes for: $packageName")
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to kill app: ${e.message}")
        }
    }
    
    private fun startContinuousBlocking(blockedApp: String?) {
        if (blockedApp == null) return
        
        // Monitor for the next 30 seconds and immediately block if the app reappears
        val continuousBlockingRunnable = object : Runnable {
            private var iterations = 0
            private val maxIterations = 15 // 30 seconds of monitoring
            
            override fun run() {
                try {
                    val currentApp = getForegroundAppPackageName()
                    if (currentApp == blockedApp) {
                        android.util.Log.w("BlockerService", "Blocked app reappeared: $blockedApp - blocking again!")
                        // showRepeatedAccessNotification()
                        
                        // Also try to show overlay blocking screen again
                        handler.postDelayed({
                            showOverlayBlockingScreen()
                        }, 500)
                    }
                    
                    iterations++
                    if (iterations < maxIterations) {
                        handler.postDelayed(this, 2000) // Check every 2 seconds
                    } else {
                        android.util.Log.d("BlockerService", "Continuous blocking monitoring ended for: $blockedApp")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BlockerService", "Error in continuous blocking: ${e.message}")
                }
            }
        }
        
        handler.postDelayed(continuousBlockingRunnable, 2000) // Start monitoring after 2 seconds
        android.util.Log.d("BlockerService", "Started continuous blocking monitoring for: $blockedApp")
    }
    
    private fun showBlockingNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create blocking channel if it doesn't exist
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "blocking_channel",
                    "App Blocking Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications shown when apps are blocked"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Create a more prominent blocking notification
            val notification = NotificationCompat.Builder(this, "blocking_channel")
                .setContentTitle("🚫 APP ACCESS BLOCKED!")
                .setContentText("Time limit reached. App has been blocked for today.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🚫 APP ACCESS BLOCKED!\n\nYou've reached your daily time limit for this app. The app has been blocked and you've been redirected to the home screen.\n\nTaking breaks from apps is healthy! 😊"))
                .setSmallIcon(android.R.drawable.ic_delete)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setOngoing(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            
            // Show notification with a unique ID so multiple blocks show separate notifications
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 1000
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("BlockerService", "Enhanced blocking notification shown with ID: $notificationId")
            
            // Also create a persistent heads-up notification that stays for longer
            handler.postDelayed({
                // showPersistentBlockingReminder()
            }, 2000)
            
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to show notification: ${e.message}")
        }
    }
    
    private fun showPersistentBlockingReminder() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val reminderNotification = NotificationCompat.Builder(this, "blocking_channel")
                .setContentTitle("⚠️ App Still Blocked")
                .setContentText("Time limit is active. Try a different activity instead!")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setTimeoutAfter(10000) // Auto-dismiss after 10 seconds
                .build()
            
            notificationManager.notify(2000, reminderNotification)
            android.util.Log.d("BlockerService", "Persistent blocking reminder shown")
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to show reminder notification: ${e.message}")
        }
    }
    
    private fun showRepeatedAccessNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val notification = NotificationCompat.Builder(this, "blocking_channel")
                .setContentTitle("🛑 REPEATED ACCESS ATTEMPT")
                .setContentText("App is still blocked! Please choose a different activity.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🛑 REPEATED ACCESS ATTEMPT\n\nThis app is still blocked due to time limits. Please try a different activity or take a break from screen time.\n\nRemember: Digital wellness is important! 💚"))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            
            // Use a different ID for repeated access notifications
            val notificationId = 3000 + (System.currentTimeMillis() % 100).toInt()
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("BlockerService", "Repeated access notification shown")
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to show repeated access notification: ${e.message}")
        }
    }
    
    private fun showBlockingActivityAsLastResort() {
        try {
            android.util.Log.d("BlockerService", "Showing blocking activity as last resort")
            
            // Show blocking activity from our app context (since we're now in foreground)
            val blockingIntent = Intent(this, BlockingActivity::class.java)
            blockingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                  Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                  Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(blockingIntent)
            
            android.util.Log.d("BlockerService", "Blocking activity shown as last resort")
            
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to show blocking activity: ${e.message}")
        }
    }

    private fun showOverlayBlockingScreen() {
        try {
            android.util.Log.d("BlockerService", "Starting overlay blocking service")
            
            // Check if we have overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    android.util.Log.w("BlockerService", "Overlay permission not granted, falling back to regular blocking")
                    // Fallback to regular blocking
                    // bringOurAppToForeground() // Removed as per edit hint
                    return
                }
            }
            
            // Start the overlay blocking service
            val overlayIntent = Intent(this, OverlayBlockingService::class.java)
            startService(overlayIntent)
            
            android.util.Log.d("BlockerService", "Overlay blocking service started")
            
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to show overlay blocking screen: ${e.message}")
            // Fallback to regular blocking
            // bringOurAppToForeground() // Removed as per edit hint
        }
    }

    private fun startForegroundServiceWithNotification() {
        try {
        val channelId = "blocker_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Blocker Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Blocker Running")
            .setContentText("Monitoring app usage and blocking as needed.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)
            android.util.Log.d("BlockerService", "Foreground service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to start foreground service: ${e.message}")
            // Try to continue as background service if foreground fails
            throw e
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if we should actually be running
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
        val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()
        
        android.util.Log.d("BlockerService", "onStartCommand - hasPermission: ${hasUsageStatsPermission(this)}, hasApps: $hasSelectedApps, hasLimits: $hasTimeLimits")
        
        if (!hasUsageStatsPermission(this) || !hasSelectedApps || !hasTimeLimits) {
            android.util.Log.d("BlockerService", "Stopping service - conditions not met")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Try to start foreground service, but continue as background if it fails
        try {
            startForegroundServiceWithNotification()
            android.util.Log.d("BlockerService", "Foreground service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("BlockerService", "Failed to start foreground service: ${e.message}")
            android.util.Log.d("BlockerService", "Continuing as background service")
            // Don't stop the service, continue as background
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }
} 