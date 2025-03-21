package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.second_year.hkroadmap.Adapters.EventsAdapter
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityHomeBinding
import com.second_year.hkroadmap.databinding.AnnouncementBadgeBinding
import com.second_year.hkroadmap.databinding.NotificationBadgeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var eventsAdapter: EventsAdapter
    private val TAG = "HomeActivity"
    private var refreshJob: Job? = null

    private var currentEvents = mutableListOf<EventResponse>()
    private var currentSortOrder = SortOrder.DATE_DESC

    private enum class SortOrder {
        DATE_ASC, DATE_DESC
    }

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 100
        private const val ANNOUNCEMENT_REQUEST_CODE = 101
        private const val REFRESH_INTERVAL = 30000L // 30 seconds
    }

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
            setupRecyclerView()
            setupViewAllRequirementsButton()
            setupSortButton()
            setupUnreadNotificationCount()
            setupUnreadAnnouncementCount()  // Add this
            fetchEvents()
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel() // Cancel any existing job
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    setupUnreadNotificationCount()
                    setupUnreadAnnouncementCount()
                    delay(REFRESH_INTERVAL)
                } catch (e: Exception) {
                    logError("Error in periodic refresh", e)
                }
            }
        }
    }



    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                setDisplayShowTitleEnabled(false) // Hide the title
            }

            // Setup profile container click
            binding.profileContainer.setOnClickListener {
                showProfileMenu(it)
            }

            // Load profile picture immediately
            loadProfilePicture(binding.menuProfileImage)
        } catch (e: Exception) {
            logError("Error setting up toolbar", e)
        }
    }
    // Add this function
    private fun setupUnreadAnnouncementCount() {
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService()
                    .getAnnouncementUnreadCount("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val unreadCount = response.body()!!.unread_count  // Changed from unreadCount to unread_count
                    updateAnnouncementBadge(unreadCount)
                }
            } catch (e: Exception) {
                logError("Failed to fetch unread announcements count", e)
            }
        }
    }

    // Add this function
    private fun updateAnnouncementBadge(count: Int) {
        try {
            val announcementMenuItem = binding.toolbar.menu.findItem(R.id.action_announcements)
            if (count > 0) {
                val actionView = AnnouncementBadgeBinding.inflate(layoutInflater)
                actionView.announcementBadgeCount.text = if (count > 99) "99+" else count.toString()

                announcementMenuItem.actionView = actionView.root
                actionView.root.setOnClickListener {
                    navigateToAnnouncements()
                }
            } else {
                announcementMenuItem.actionView = null
            }
        } catch (e: Exception) {
            logError("Error updating announcement badge", e)
        }
    }

    private fun setupUnreadNotificationCount() {
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val authToken = "Bearer $token"
                val response = RetrofitInstance.createApiService().getStudentUnreadCount(authToken)
                if (response.isSuccessful && response.body() != null) {
                    updateNotificationBadge(response.body()!!.unread_count)
                }
            } catch (e: Exception) {
                logError("Failed to fetch unread notifications count", e)
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        try {
            val notificationMenuItem = binding.toolbar.menu.findItem(R.id.action_notifications)
            if (count > 0) {
                // Try using View Binding instead of findViewById
                val actionView = NotificationBadgeBinding.inflate(layoutInflater)
                actionView.badgeCount.text = if (count > 99) "99+" else count.toString()
                notificationMenuItem.actionView = actionView.root
                actionView.root.setOnClickListener {
                    navigateToNotifications()
                }
            } else {
                notificationMenuItem.actionView = null
            }
        } catch (e: Exception) {
            logError("Error updating notification badge", e)
        }
    }

    private fun navigateToNotifications() {
        try {
            Intent(this, NotificationActivity::class.java).also { intent ->
                startActivityForResult(intent, NOTIFICATION_REQUEST_CODE)
            }
        } catch (e: Exception) {
            logError("Error navigating to Notifications", e)
            showToast("Unable to view notifications")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            NOTIFICATION_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    setupUnreadNotificationCount()
                }
            }
            ANNOUNCEMENT_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    setupUnreadAnnouncementCount()
                }
            }
        }
    }

    // Update this function
    private fun navigateToAnnouncements() {
        try {
            Intent(this, AnnouncementActivity::class.java).also { intent ->
                startActivityForResult(intent, ANNOUNCEMENT_REQUEST_CODE)
            }
        } catch (e: Exception) {
            logError("Error navigating to Announcements", e)
            showToast("Unable to view announcements")
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

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            showSortOptions(it)
        }
    }

    private fun showSortOptions(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.sort_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_date_asc -> {
                        currentSortOrder = SortOrder.DATE_ASC
                        sortAndDisplayEvents()
                        true
                    }
                    R.id.sort_date_desc -> {
                        currentSortOrder = SortOrder.DATE_DESC
                        sortAndDisplayEvents()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun sortAndDisplayEvents() {
        try {
            val sortedEvents = when (currentSortOrder) {
                SortOrder.DATE_ASC -> currentEvents.sortedBy { parseEventDate(it.date) }
                SortOrder.DATE_DESC -> currentEvents.sortedByDescending { parseEventDate(it.date) }
            }
            eventsAdapter.submitList(sortedEvents)
        } catch (e: Exception) {
            logError("Error sorting events", e)
            showToast("Error sorting events")
        }
    }

    private fun parseEventDate(dateString: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(dateString) ?: Date(0)
        } catch (e: Exception) {
            logError("Error parsing date: $dateString", e)
            Date(0)
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
                    Log.d(
                        TAG,
                        "Found ${validRequirements.size} valid requirements for event: ${event.id}"
                    )
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

                currentEvents = events.mapNotNull { eventItem ->
                    try {
                        EventResponse(
                            id = eventItem.event_id,
                            title = eventItem.event_name,
                            description = "No description available",
                            date = eventItem.date,
                            created_at = eventItem.date,
                            updated_at = eventItem.date
                        )
                    } catch (e: Exception) {
                        logError("Error converting event: ${eventItem}", e)
                        null
                    }
                }.toMutableList()

                if (currentEvents.isEmpty()) {
                    Log.w(TAG, "No events available after conversion")
                    showToast("No events available")
                } else {
                    Log.d(TAG, "Setting ${currentEvents.size} events to adapter")
                    sortAndDisplayEvents() // Use sorting function instead of direct submitList
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


    private fun logError(message: String, throwable: Throwable?) {
        Log.e(
            TAG, """
            Error: $message
            Stack trace: ${throwable?.stackTraceToString() ?: "No stack trace"}
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android version: ${Build.VERSION.RELEASE}
            Package: ${packageName}
            Version: ${packageManager.getPackageInfo(packageName, 0).versionName}
        """.trimIndent()
        )
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                navigateToNotifications()
                true
            }
            R.id.action_announcements -> {
                navigateToAnnouncements()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showProfileMenu(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.profile_menu, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_view_profile -> {
                        navigateToProfile()
                        true
                    }
                    R.id.action_logout -> {
                        handleLogout()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun navigateToProfile() {
        try {
            Intent(this, ProfileActivity::class.java).also { intent ->
                startActivity(intent)
            }
        } catch (e: Exception) {
            logError("Error navigating to Profile", e)
            showToast("Unable to view profile")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    private fun loadProfilePicture(profileImageView: ShapeableImageView) {
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService().getProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    profile.profilePictureUrl?.let { fileName ->
                        val imageUrl = RetrofitInstance.getProfilePictureUrl(fileName)
                        Glide.with(this@HomeActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(profileImageView)
                    } ?: run {
                        // Load default profile icon if no profile picture
                        Glide.with(this@HomeActivity)
                            .load(R.drawable.ic_profile)
                            .circleCrop()
                            .into(profileImageView)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile picture", e)
            }
        }
    }

    // Add this function to refresh the profile picture when needed
    fun refreshProfilePicture() {
        loadProfilePicture(binding.menuProfileImage)
    }

    override fun onResume() {
        super.onResume()
        fetchEvents()
        setupUnreadNotificationCount()
        setupUnreadAnnouncementCount()
        refreshProfilePicture()
        startPeriodicRefresh() // Restart periodic updates
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel() // Stop updates when activity is not visible
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
    }
}
