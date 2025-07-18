package com.example.testing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnalyticsAdapter(private val appUsageList: List<AnalyticsActivity.AppUsageData>) : 
    RecyclerView.Adapter<AnalyticsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appNameText: TextView = view.findViewById(R.id.appNameText)
        val usageText: TextView = view.findViewById(R.id.usageText)
        val limitText: TextView = view.findViewById(R.id.limitText)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val percentageText: TextView = view.findViewById(R.id.percentageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analytics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appUsage = appUsageList[position]
        
        holder.appNameText.text = appUsage.appName
        
        // Format usage time
        val usageHours = appUsage.usageMinutes / 60
        val usageMins = appUsage.usageMinutes % 60
        holder.usageText.text = if (usageHours > 0) {
            "${usageHours}h ${usageMins}m"
        } else {
            "${usageMins}m"
        }
        
        // Format limit time
        val limitHours = appUsage.limitMinutes / 60
        val limitMins = appUsage.limitMinutes % 60
        holder.limitText.text = if (limitHours > 0) {
            "of ${limitHours}h ${limitMins}m"
        } else {
            "of ${limitMins}m"
        }
        
        // Calculate and display progress
        val percentage = if (appUsage.limitMinutes > 0) {
            (appUsage.usageMinutes * 100 / appUsage.limitMinutes).coerceAtMost(100)
        } else {
            0
        }
        
        holder.progressBar.progress = percentage
        holder.percentageText.text = "${percentage}%"
        
        // Color coding based on usage
        when {
            percentage >= 100 -> {
                holder.percentageText.setTextColor(holder.itemView.context.getColor(R.color.colorError))
                holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.colorError)
                )
            }
            percentage >= 80 -> {
                holder.percentageText.setTextColor(holder.itemView.context.getColor(R.color.colorSecondary))
                holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.colorSecondary)
                )
            }
            else -> {
                holder.percentageText.setTextColor(holder.itemView.context.getColor(R.color.colorPrimary))
                holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.colorPrimary)
                )
            }
        }
    }

    override fun getItemCount() = appUsageList.size
} 