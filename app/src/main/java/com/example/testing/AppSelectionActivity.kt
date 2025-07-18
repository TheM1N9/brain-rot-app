package com.example.testing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView

class AppSelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var adapter: AppListAdapter
    private lateinit var appList: List<AppInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LayoutInflater.from(this).inflate(R.layout.activity_app_selection, null)
        setContentView(root)

        recyclerView = root.findViewById(R.id.recyclerView)
        saveButton = root.findViewById(R.id.saveButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        appList = getInstalledApps()
        adapter = AppListAdapter(appList)
        recyclerView.adapter = adapter

        saveButton.setOnClickListener {
            val selectedApps = adapter.getSelectedApps()
            saveSelectedApps(selectedApps)
            Toast.makeText(this, "Saved ${selectedApps.size} apps to block", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val myPackage = packageName
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos
            .filter { it.activityInfo.packageName != myPackage }
            .map {
                val appInfo = pm.getApplicationInfo(it.activityInfo.packageName, 0)
                AppInfo(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    appInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.appName }
    }

    private fun saveSelectedApps(selectedApps: List<AppInfo>) {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet("blocked_packages", selectedApps.map { it.packageName }.toSet())
        editor.apply()
    }
}

data class AppInfo(val appName: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class AppListAdapter(private val apps: List<AppInfo>) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    private val selected = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.appName
        holder.appIcon.setImageDrawable(app.icon)
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selected.contains(app.packageName)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selected.add(app.packageName) else selected.remove(app.packageName)
        }
        // Removed itemView click toggling to avoid double toggling issues
    }

    override fun getItemCount() = apps.size

    fun getSelectedApps(): List<AppInfo> = apps.filter { selected.contains(it.packageName) }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
    }
} 