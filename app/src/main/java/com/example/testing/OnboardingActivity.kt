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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
        private const val PREFS_NAME = "onboarding"
        private const val ONBOARDING_COMPLETE_KEY = "onboarding_complete"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: MaterialButton
    private lateinit var getStartedButton: MaterialButton

    private val onboardingPages = listOf(
        OnboardingPage(
            "Welcome to Brain Detox",
            "Take control of your digital habits and reduce mindless scrolling. Set limits, block distractions, and focus on what matters most.",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Set App Limits",
            "Choose which apps distract you most and set daily time limits. When you reach your limit, we'll help you take a break.",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Smart Blocking",
            "Our intelligent system detects when you're using blocked apps and gently redirects you to more productive activities.",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Track Your Progress",
            "Monitor your daily screen time and see how you're improving. Celebrate your wins and stay motivated.",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Permissions Required",
            "To help you stay focused, we need a few permissions. Don't worry - we only use them to track app usage and show blocking screens.",
            R.drawable.logo_no_bg
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Check if onboarding is already complete
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(ONBOARDING_COMPLETE_KEY, false)) {
            startMainActivity()
            return
        }

        setupViews()
        setupViewPager()
        setupButtons()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        nextButton = findViewById(R.id.nextButton)
        skipButton = findViewById(R.id.skipButton)
        getStartedButton = findViewById(R.id.getStartedButton)
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(onboardingPages)
        viewPager.adapter = adapter

        // Connect ViewPager with TabLayout
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Handle page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
    }

    private fun setupButtons() {
        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingPages.size - 1) {
                viewPager.currentItem += 1
            }
        }

        skipButton.setOnClickListener {
            startPermissionFlow()
        }

        getStartedButton.setOnClickListener {
            startPermissionFlow()
        }

        updateButtons(0)
    }

    private fun updateButtons(position: Int) {
        when (position) {
            onboardingPages.size - 1 -> {
                // Last page - show "Get Started" button
                nextButton.visibility = View.GONE
                skipButton.visibility = View.GONE
                getStartedButton.visibility = View.VISIBLE
            }
            else -> {
                // Other pages - show "Next" and "Skip" buttons
                nextButton.visibility = View.VISIBLE
                skipButton.visibility = View.VISIBLE
                getStartedButton.visibility = View.GONE
            }
        }
    }

    private fun startPermissionFlow() {
        // Start permission request flow
        requestNotificationPermission()
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
            } else {
                requestUsageAccessPermission()
            }
        } else {
            requestUsageAccessPermission()
        }
    }

    private fun requestUsageAccessPermission() {
        if (!hasUsageStatsPermission(this)) {
            showPermissionDialog(
                "Usage Access Permission",
                "To track your app usage and enforce time limits, we need access to your usage statistics. This helps us know when you're using apps and for how long.",
                "Grant Permission"
            ) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog(
                    "Overlay Permission",
                    "To show blocking screens over other apps when you reach your time limits, we need overlay permission. This allows us to display a gentle reminder directly over the app you're trying to use.",
                    "Grant Permission"
                ) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                }
            } else {
                completeOnboarding()
            }
        } else {
            completeOnboarding()
        }
    }

    private fun showPermissionDialog(title: String, message: String, positiveButtonText: String, onPositiveClick: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositiveClick()
            }
            .setNegativeButton("Skip for now") { _, _ ->
                completeOnboarding()
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                requestUsageAccessPermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                completeOnboarding()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted while in settings
        if (hasUsageStatsPermission(this)) {
            requestOverlayPermission()
        }
    }

    private fun completeOnboarding() {
        // Mark onboarding as complete
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ONBOARDING_COMPLETE_KEY, true).apply()
        
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

    data class OnboardingPage(
        val title: String,
        val description: String,
        val iconResId: Int
    )
} 