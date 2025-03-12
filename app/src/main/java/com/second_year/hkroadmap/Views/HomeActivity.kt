package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.second_year.hkroadmap.Adapters.EventsAdapter
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var eventsAdapter: EventsAdapter
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            logError("Fatal error", throwable)
        }

        try {
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupNavigationDrawer()
            setupRecyclerView()
            setupViewAllRequirementsButton()
            fetchEvents()
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeAsUpIndicator(R.drawable.ic_menu)
                title = getString(R.string.app_name)
            }
        } catch (e: Exception) {
            logError("Error setting up toolbar", e)
        }
    }

    private fun setupNavigationDrawer() {
        try {
            drawerLayout = binding.drawerLayout
            val navigationView = binding.navigationView

            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        drawerLayout.closeDrawers()
                        true
                    }
                    R.id.nav_logout -> {
                        handleLogout()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            logError("Error setting up navigation drawer", e)
        }
    }

    private fun setupViewAllRequirementsButton() {
        try {
            binding.viewAllRequirementsBtn.setOnClickListener {
                Log.d(TAG, "View All Requirements button clicked")
                navigateToViewRequirements()
            }
        } catch (e: Exception) {
            logError("Error setting up View All Requirements button", e)
        }
    }

    private fun navigateToViewRequirements() {
        try {
            Intent(this, ViewRequirementsActivity::class.java).also { intent ->
                startActivity(intent)
            }
        } catch (e: Exception) {
            logError("Error navigating to View Requirements", e)
            showToast("Unable to view requirements")
        }
    }

    private fun setupRecyclerView() {
        try {
            eventsAdapter = EventsAdapter().apply {
                setOnEventClickListener { event ->
                    checkEventRequirements(event)
                }
            }

            binding.eventsRecyclerView.apply {
                adapter = eventsAdapter
                layoutManager = LinearLayoutManager(this@HomeActivity)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } catch (e: Exception) {
            logError("Error setting up RecyclerView", e)
        }
    }

    private fun checkEventRequirements(event: EventResponse) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showToast("Authentication error")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Checking requirements for event: ${event.id}")
                val authToken = "Bearer $token"
                val requirements = RetrofitInstance.createApiService()
                    .getRequirementsByEventId(authToken, event.id)

                val validRequirements = requirements.filter { it.event_id == event.id }

                if (validRequirements.isEmpty()) {
                    Log.d(TAG, "No valid requirements found for event: ${event.id}")
                    showToast("No requirements found for this event")
                } else {
                    Log.d(TAG, "Found ${validRequirements.size} valid requirements for event: ${event.id}")
                    navigateToRequirements(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking requirements", e)
                showToast("Error checking requirements")
            }
        }
    }

    private fun navigateToRequirements(event: EventResponse) {
        Intent(this@HomeActivity, RequirementActivity::class.java).also { intent ->
            intent.putExtra("event_id", event.id)
            intent.putExtra("event_title", event.title)
            intent.putExtra("event_date", event.date)
            intent.putExtra("event_location", event.location ?: "TBD")
            startActivity(intent)
        }
    }

    private fun fetchEvents() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            logError("Authentication error", null)
            showToast("Authentication error")
            navigateToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching events...")
                val authToken = "Bearer $token"
                val events = RetrofitInstance.createApiService().getEvents(authToken)
                Log.d(TAG, "Received events: ${events.size}")

                val eventResponses = events.mapNotNull { eventItem ->
                    try {
                        EventResponse(
                            id = eventItem.event_id,
                            title = eventItem.event_name,
                            description = "No description available",
                            date = eventItem.date,
                            location = "TBD",
                            created_at = eventItem.date,
                            updated_at = eventItem.date
                        ).also {
                            Log.d(TAG, "Converted event: $it")
                        }
                    } catch (e: Exception) {
                        logError("Error converting event: ${eventItem}", e)
                        null
                    }
                }

                if (eventResponses.isEmpty()) {
                    Log.w(TAG, "No events available after conversion")
                    showToast("No events available")
                } else {
                    Log.d(TAG, "Setting ${eventResponses.size} events to adapter")
                    eventsAdapter.submitList(eventResponses)
                }
            } catch (e: Exception) {
                logError("Failed to fetch events", e)
                showToast("Failed to load events: ${e.message}")
            }
        }
    }

    private fun handleLogout() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.w(TAG, "No token found, proceeding with local logout")
            performLocalLogout()
            return
        }

        lifecycleScope.launch {
            try {
                val authToken = "Bearer $token"
                val response = RetrofitInstance.createApiService().studentLogout(authToken)
                Log.d(TAG, "Logout response: ${response.message}")

                if (response.message.contains("success", ignoreCase = true)) {
                    performLocalLogout()
                } else {
                    logError("Logout failed with message: ${response.message}", null)
                    showToast("Logout failed: ${response.message}")
                }
            } catch (e: Exception) {
                logError("Logout failed", e)
                showToast("Logout failed: ${e.message}")
                performLocalLogout()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logError(message: String, throwable: Throwable?) {
        Log.e(TAG, """
            Error: $message
            Stack trace: ${throwable?.stackTraceToString() ?: "No stack trace"}
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android version: ${Build.VERSION.RELEASE}
            Package: ${packageName}
            Version: ${packageManager.getPackageInfo(packageName, 0).versionName}
        """.trimIndent())
    }

    private fun performLocalLogout() {
        TokenManager.clearToken(this)
        navigateToLogin()
    }

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchEvents()
    }

    //Temporary, remove this if we want to add functionality to the notification icon
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }
}
