package com.example.testing

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent

class TimeLimitActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var adapter: TimeLimitAdapter
    private lateinit var appList: List<AppInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LayoutInflater.from(this).inflate(R.layout.activity_time_limit, null)
        setContentView(root)

        recyclerView = root.findViewById(R.id.recyclerView)
        saveButton = root.findViewById(R.id.saveButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val specificAppPackage = intent.getStringExtra("specific_app_package")
        if (specificAppPackage != null) {
            // Show only the specific app that was blocked
            appList = loadSpecificApp(specificAppPackage)
        } else {
            // Show all selected apps (normal flow)
            appList = loadSelectedApps()
        }
        
        val timeLimits = loadTimeLimits()
        adapter = TimeLimitAdapter(appList, timeLimits)
        recyclerView.adapter = adapter

        saveButton.setOnClickListener {
            val limits = adapter.getTimeLimits()
            saveTimeLimits(limits)
            Toast.makeText(this, "Time limits saved", Toast.LENGTH_SHORT).show()
            
            // If this was triggered from a blocked app, redirect back to it
            if (specificAppPackage != null) {
                redirectToApp(specificAppPackage)
            } else {
                finish()
            }
        }
    }

    private fun loadSelectedApps(): List<AppInfo> {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val pkgs = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val pm = packageManager
        return pkgs.mapNotNull { pkg ->
            try {
                val app = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    app.loadLabel(pm).toString(),
                    app.packageName,
                    app.loadIcon(pm)
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName }
    }

    private fun loadTimeLimits(): Map<String, Int> {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val map = mutableMapOf<String, Int>()
        prefs.getString("time_limits", null)?.split("|")?.forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) map[parts[0]] = parts[1].toIntOrNull() ?: 0
        }
        return map
    }

    private fun saveTimeLimits(limits: Map<String, Int>) {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val str = limits.entries.joinToString("|") { "${it.key},${it.value}" }
        prefs.edit().putString("time_limits", str).apply()
    }

    private fun loadSpecificApp(packageName: String): List<AppInfo> {
        val pm = packageManager
        return try {
            val app = pm.getApplicationInfo(packageName, 0)
            listOf(AppInfo(
                app.loadLabel(pm).toString(),
                app.packageName,
                app.loadIcon(pm)
            ))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun redirectToApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // If we can't launch the app, just finish this activity
        }
        finish()
    }
}

class TimeLimitAdapter(
    private val apps: List<AppInfo>,
    private val timeLimits: Map<String, Int>
) : RecyclerView.Adapter<TimeLimitAdapter.ViewHolder>() {
    private val limits = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_limit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.appName
        holder.appIcon.setImageDrawable(app.icon)
        val limit = timeLimits[app.packageName] ?: 0
        holder.timeLimit.setText(if (limit > 0) limit.toString() else "")
        holder.timeLimit.inputType = InputType.TYPE_CLASS_NUMBER
        holder.timeLimit.hint = "Minutes"
        holder.timeLimit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = holder.timeLimit.text.toString().toIntOrNull() ?: 0
                limits[app.packageName] = value
            }
        }
    }

    override fun getItemCount() = apps.size

    fun getTimeLimits(): Map<String, Int> {
        // Ensure latest values are captured
        for (i in 0 until itemCount) {
            val holder = recyclerView?.findViewHolderForAdapterPosition(i) as? ViewHolder
            holder?.let {
                val value = it.timeLimit.text.toString().toIntOrNull() ?: 0
                limits[apps[i].packageName] = value
            }
        }
        return limits
    }

    private var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        super.onAttachedToRecyclerView(rv)
        recyclerView = rv
    }
    override fun onDetachedFromRecyclerView(rv: RecyclerView) {
        super.onDetachedFromRecyclerView(rv)
        recyclerView = null
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val timeLimit: EditText = view.findViewById(R.id.timeLimit)
    }
} 