package com.second_year.hkroadmap.Views

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.second_year.hkroadmap.Adapters.WelcomeViewPagerAdapter
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var adapter: WelcomeViewPagerAdapter
    private lateinit var indicators: Array<ImageView>

    companion object {
        private const val PREF_NAME = "app_prefs"
        private const val KEY_FIRST_TIME = "is_first_time"

        fun isFirstTime(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_FIRST_TIME, true)
        }

        fun setFirstTimeDone(context: Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupIndicators()
        setupButtons()
    }

    private fun setupViewPager() {
        adapter = WelcomeViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                updateButtonsVisibility(position)
            }
        })
    }

    private fun setupIndicators() {
        indicators = Array(adapter.itemCount) { ImageView(this) }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(this)
            indicators[i].setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_inactive
                )
            )
            indicators[i].layoutParams = params
            binding.indicatorsContainer.addView(indicators[i])
        }

        // Set first indicator as active
        if (indicators.isNotEmpty()) {
            indicators[0].setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_active
                )
            )
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in indicators.indices) {
            indicators[i].setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            )
        }
    }

    private fun updateButtonsVisibility(position: Int) {
        // Show "Get Started" button only on the last page
        binding.btnGetStarted.visibility = if (position == adapter.itemCount - 1) {
            android.view.View.VISIBLE
        } else {
            android.view.View.INVISIBLE
        }

        // Show "Skip" button except on the last page
        binding.btnSkip.visibility = if (position == adapter.itemCount - 1) {
            android.view.View.INVISIBLE
        } else {
            android.view.View.VISIBLE
        }

        // Show "Next" button except on the last page
        binding.btnNext.visibility = if (position == adapter.itemCount - 1) {
            android.view.View.INVISIBLE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            }
        }

        binding.btnSkip.setOnClickListener {
            finishWelcomeScreen()
        }

        binding.btnGetStarted.setOnClickListener {
            finishWelcomeScreen()
        }
    }

    fun resetFirstTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_TIME, true).apply()
    }

    private fun finishWelcomeScreen() {
        // Mark first time as done
        setFirstTimeDone(this)

        // Check if user is already logged in
        val token = TokenManager.getToken(this)
        val intent = if (token != null) {
            // User is logged in, go to home screen
            Intent(this, HomeActivity::class.java)
        } else {
            // User is not logged in, go to login screen
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}