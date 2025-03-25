package com.second_year.hkroadmap.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Models.WelcomeSlide

class WelcomeViewPagerAdapter(private val context: Context) :
    RecyclerView.Adapter<WelcomeViewPagerAdapter.WelcomeViewHolder>() {

    private val slides = listOf(
        WelcomeSlide(
            R.drawable.welcome_1,
            "Welcome to HK Roadmap",
            "Your guide to navigating PHINMA University of Pangasinan - Hawak Kamay scholarship requirements"
        ),
        WelcomeSlide(
            R.drawable.welcome_2,
            "Track Your Progress",
            "Keep track of your scholarship requirements and files"
        ),
        WelcomeSlide(
            R.drawable.welcome_3,
            "Stay Updated",
            "Receive notifications about important announcements, events and deadlines"
        ),
        // Credits slide with welcome_4 image
        WelcomeSlide(
            R.drawable.welcome_4,
            "Credits",
            "Meet the team behind HK Roadmap"
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WelcomeViewHolder {
        val view = if (viewType == VIEW_TYPE_CREDITS) {
            LayoutInflater.from(context).inflate(R.layout.item_welcome_credits, parent, false)
        } else {
            LayoutInflater.from(context).inflate(R.layout.item_welcome_slide, parent, false)
        }
        return WelcomeViewHolder(view, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == slides.size - 1) VIEW_TYPE_CREDITS else VIEW_TYPE_NORMAL
    }

    override fun onBindViewHolder(holder: WelcomeViewHolder, position: Int) {
        val slide = slides[position]
        holder.bind(slide)
    }

    override fun getItemCount(): Int = slides.size

    inner class WelcomeViewHolder(itemView: View, private val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView? = itemView.findViewById(R.id.welcomeImage)
        private val titleTextView: TextView? = itemView.findViewById(R.id.welcomeTitle)
        private val descriptionTextView: TextView? = itemView.findViewById(R.id.welcomeDescription)
        private val creditsTextView: TextView? = itemView.findViewById(R.id.creditsText)

        fun bind(slide: WelcomeSlide) {
            if (viewType == VIEW_TYPE_NORMAL) {
                imageView?.setImageResource(slide.imageResId)
                titleTextView?.text = slide.title
                descriptionTextView?.text = slide.description
            } else {
                // For credits slide
                imageView?.setImageResource(slide.imageResId) // Set the welcome_4 image in the credits slide
                titleTextView?.text = slide.title
                descriptionTextView?.text = slide.description
                creditsTextView?.text = """
                    Ace Philip S. Denulan
                    Justin Paul Louise C. Escano
                    Princess P. Caguioa
                    Feniel A. Barte
                    Shiela Mae L. Basa
                    John Ryan O. Reyes
                """.trimIndent()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_CREDITS = 1
    }
}