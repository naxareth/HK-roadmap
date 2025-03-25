package com.second_year.hkroadmap.Views

import android.animation.Animator
import android.animation.AnimatorInflater
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Fragments.ProfileFragment
import com.second_year.hkroadmap.Fragments.RequirementFragment
import com.second_year.hkroadmap.Fragments.StatusFragment
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityRequirementBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RequirementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementBinding
    private var previousSelectedItem: MenuItem? = null
    private var currentAnimator: Animator? = null

    companion object {
        private const val TAG = "RequirementActivity"
    }

    private var eventId: Int = -1
    private var eventTitle: String = ""
    private var eventDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Activity created")

        // Get event details from intent
        intent.extras?.let { extras ->
            eventId = extras.getInt("event_id", -1)
            eventTitle = extras.getString("event_title", "")
            eventDate = extras.getString("event_date", "")?.let { formatDate(it) } ?: ""

            Log.d(TAG, """
                Received event details:
                - ID: $eventId
                - Title: $eventTitle
                - Date: $eventDate
            """.trimIndent())
        }

        if (eventId == -1) {
            Log.e(TAG, "Invalid event ID received")
            showError(getString(R.string.error_invalid_event))
            finish()
            return
        }

        setupToolbar()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadInitialFragment()
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateString", e)
            dateString
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = eventTitle
        }
        Log.d(TAG, "Toolbar setup completed with title: $eventTitle")
    }

    private fun loadInitialFragment() {
        val requirementFragment = RequirementFragment.newInstance(
            eventId,
            eventTitle,
            eventDate,
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, requirementFragment, "requirements")
            .commit()
    }

    private fun loadStatusFragment() {
        Log.d(TAG, "Loading status fragment for event: $eventId")
        val statusFragment = StatusFragment.newInstance(eventId)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, statusFragment, "status")
            .commit()
    }

    private fun setupBottomNavigation() {
        // Set initial selection
        binding.bottomNavigation.selectedItemId = R.id.nav_requirements

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Cancel any ongoing animation
            currentAnimator?.cancel()

            // Animate the previously selected item back to original position
            previousSelectedItem?.let { previousItem ->
                animateNavigationItem(previousItem, false)
            }

            // Animate the newly selected item
            animateNavigationItem(item, true)

            // Store the newly selected item for next time
            previousSelectedItem = item

            when (item.itemId) {
                R.id.nav_requirements -> {
                    Log.d(TAG, "Navigation: Requirements selected")
                    val currentFragment = supportFragmentManager.findFragmentByTag("requirements")
                    if (currentFragment == null || !currentFragment.isVisible) {
                        loadInitialFragment()
                    }
                    true
                }
                R.id.nav_status -> {
                    Log.d(TAG, "Navigation: Status selected")
                    loadStatusFragment()
                    true
                }
                R.id.nav_profile -> {
                    Log.d(TAG, "Navigation: Profile selected")
                    loadProfileFragment()
                    true
                }
                else -> false
            }
        }

        // Set the initial selected item as previously selected
        previousSelectedItem = binding.bottomNavigation.menu.findItem(R.id.nav_requirements)
        // Animate the initial selection
        animateNavigationItem(previousSelectedItem!!, true)
    }

    private fun animateNavigationItem(item: MenuItem, selected: Boolean) {
        try {
            // Find the icon view for this menu item
            val itemView = binding.bottomNavigation.findViewById<View>(item.itemId)

            // Load the appropriate animator
            val animator = AnimatorInflater.loadAnimator(
                this,
                if (selected) R.animator.bottom_nav_item_animator
                else R.animator.bottom_nav_item_animator_reverse
            )

            // Set the target and start the animation
            animator.setTarget(itemView)
            animator.start()

            // Store the current animator
            currentAnimator = animator

            // Update the icon tint - now using white for selected state
            if (selected) {
                item.icon?.setTint(ContextCompat.getColor(this, R.color.white))
            } else {
                item.icon?.setTint(ContextCompat.getColor(this, R.color.gray))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error animating navigation item", e)
        }
    }

    private fun loadProfileFragment() {
        Log.d(TAG, "Loading profile fragment")
        try {
            val profileFragment = ProfileFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, profileFragment, "profile")
                .commit()
            Log.d(TAG, "Profile fragment transaction committed")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile fragment", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d(TAG, "Back navigation selected")
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentAnimator?.cancel()
    }
}