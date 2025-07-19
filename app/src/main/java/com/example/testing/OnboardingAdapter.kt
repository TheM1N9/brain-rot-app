package com.example.testing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(private val pages: List<OnboardingActivity.OnboardingPage>) : 
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.onboardingIcon)
        val title: TextView = itemView.findViewById(R.id.onboardingTitle)
        val description: TextView = itemView.findViewById(R.id.onboardingDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.title.text = page.title
        holder.description.text = page.description
        holder.icon.setImageResource(page.iconResId)
    }

    override fun getItemCount(): Int = pages.size
} 