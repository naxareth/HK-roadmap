package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Fragments.ProfileFragment
import com.second_year.hkroadmap.Fragments.RequirementFragment  // This is the correct import
import com.second_year.hkroadmap.Fragments.StatusFragment
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityRequirementBinding
class RequirementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementBinding

    companion object {
        private const val TAG = "RequirementActivity"
    }

    private var eventId: Int = -1
    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Activity created")

        // Get event details from intent
        intent.extras?.let { extras ->
            eventId = extras.getInt("event_id", -1)
            eventTitle = extras.getString("event_title", "")
            eventDate = extras.getString("event_date", "")
            eventLocation = extras.getString("event_location", "TBD")

            Log.d(TAG, """
                Received event details:
                - ID: $eventId
                - Title: $eventTitle
                - Date: $eventDate
                - Location: $eventLocation
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
            eventLocation
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
                    true  // Now returning true since we're implementing it
                }
                else -> false
            }
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
}