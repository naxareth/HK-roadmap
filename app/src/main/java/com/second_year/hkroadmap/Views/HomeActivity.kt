package com.second_year.hkroadmap.Views

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
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
    private var networkJob: Job? = null

    private var currentEvents = mutableListOf<EventResponse>()
    private var currentSortOrder = SortOrder.DATE_DESC

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkAvailable = true

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

            setupNetworkMonitoring()
            setupToolbar()
            setupRecyclerView()
            setupScrollIndicator()
            setupViewAllRequirementsButton()
            setupSortButton()
            setupUnreadNotificationCount()
            setupUnreadAnnouncementCount()
            setupRetryButton()

            // Initial check for network and fetch events
            if (isNetworkAvailable()) {
                fetchEvents()
            } else {
                showNetworkError()
            }
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
        startPeriodicRefresh()
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check initial network state
        isNetworkAvailable = isNetworkAvailable()

        // Setup network callback
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                runOnUiThread {
                    if (!isNetworkAvailable) {
                        isNetworkAvailable = true
                        showNetworkRestored()
                        fetchEvents()
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                runOnUiThread {
                    isNetworkAvailable = false
                    showNetworkError()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    private fun isNetworkAvailable(): Boolean {
        try {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.activeNetwork
            )
            return networkCapabilities != null && (
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            return false
        }
    }


    private fun showNetworkError() {
        try {
            // Access the networkErrorView from the binding and set its visibility
            val networkErrorView = findViewById<View>(R.id.networkErrorView)
            networkErrorView?.visibility = View.VISIBLE

            // Hide the scroll view and indicator
            binding.nestedScrollView.visibility = View.GONE
            binding.scrollIndicator.visibility = View.GONE

            // Hide all interactive elements
            binding.viewAllRequirementsBtn.visibility = View.GONE
            binding.sortButton.visibility = View.GONE
            binding.profileContainer?.isEnabled = false

            // Safely disable menu items if menu is initialized
            binding.toolbar.menu?.let { menu ->
                for (i in 0 until menu.size()) {
                    menu.getItem(i)?.isEnabled = false
                }
            }

            // Show only the retry button
            val retryButton = findViewById<View>(R.id.buttonRetry)
            retryButton?.visibility = View.VISIBLE

            Log.d(TAG, "Network error UI updated")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing network error UI", e)
        }
    }

    private fun showNetworkRestored() {
        try {
            // Access the networkErrorView from the binding and set its visibility
            val networkErrorView = findViewById<View>(R.id.networkErrorView)
            networkErrorView?.visibility = View.GONE

            // Show the scroll view
            binding.nestedScrollView.visibility = View.VISIBLE

            // Restore all interactive elements
            binding.viewAllRequirementsBtn.visibility = View.VISIBLE
            binding.sortButton.visibility = View.VISIBLE
            binding.profileContainer?.isEnabled = true

            // Safely enable menu items if menu is initialized
            binding.toolbar.menu?.let { menu ->
                for (i in 0 until menu.size()) {
                    menu.getItem(i)?.isEnabled = true
                }
            }

            // Show a snackbar only if the activity is still active
            if (!isFinishing && !isDestroyed) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.connection_restored),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            // Check scroll indicator after showing content
            binding.nestedScrollView.post {
                checkScrollIndicatorVisibility()
            }

            Log.d(TAG, "Network restored UI updated")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing network restored UI", e)
        }
    }

    private fun setupRetryButton() {
        val retryButton = findViewById<View>(R.id.buttonRetry)
        retryButton.setOnClickListener {
            if (isNetworkAvailable()) {
                showNetworkRestored()
                fetchEvents()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.no_internet_connection),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupScrollIndicator() {
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val child = v.getChildAt(0)
            if (child != null) {
                val childHeight = child.height
                val scrollViewHeight = v.height
                val isScrollable = childHeight > scrollViewHeight
                val hasReachedBottom = scrollY >= childHeight - scrollViewHeight

                if (isScrollable && !hasReachedBottom) {
                    if (binding.scrollIndicator.visibility != View.VISIBLE) {
                        binding.scrollIndicator.show()
                    }
                } else {
                    if (binding.scrollIndicator.visibility == View.VISIBLE) {
                        binding.scrollIndicator.hide()
                    }
                }
            }
        })

        // Scroll to bottom when indicator is clicked
        binding.scrollIndicator.setOnClickListener {
            binding.nestedScrollView.post {
                binding.nestedScrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel() // Cancel any existing job
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    if (isNetworkAvailable) {
                        setupUnreadNotificationCount()
                        setupUnreadAnnouncementCount()
                    }
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

    private fun setupUnreadAnnouncementCount() {
        val token = TokenManager.getToken(this) ?: return

        networkJob?.cancel()
        networkJob = lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService()
                    .getAnnouncementUnreadCount("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val unreadCount = response.body()!!.unread_count
                    updateAnnouncementBadge(unreadCount)
                }
            } catch (e: Exception) {
                logError("Failed to fetch unread announcements count", e)
            }
        }
    }

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

            // Check if we need to show the scroll indicator after sorting
            binding.nestedScrollView.post {
                checkScrollIndicatorVisibility()
            }
        } catch (e: Exception) {
            logError("Error sorting events", e)
            showToast("Error sorting events")
        }
    }

    private fun checkScrollIndicatorVisibility() {
        val scrollView = binding.nestedScrollView
        val child = scrollView.getChildAt(0)

        if (child != null) {
            val childHeight = child.height
            val scrollViewHeight = scrollView.height
            val scrollY = scrollView.scrollY
            val isScrollable = childHeight > scrollViewHeight
            val hasReachedBottom = scrollY >= childHeight - scrollViewHeight

            binding.scrollIndicator.visibility = if (isScrollable && !hasReachedBottom) View.VISIBLE else View.GONE
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
        if (!isNetworkAvailable) {
            showToast(getString(R.string.no_internet_connection))
            return
        }

        // Always navigate to the requirements screen
        navigateToRequirements(event)

        // Optionally log requirement check in the background
        val token = TokenManager.getToken(this)
        if (token != null) {
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Checking requirements for event: ${event.id}")
                    val authToken = "Bearer $token"
                    val requirements = RetrofitInstance.createApiService()
                        .getRequirementsByEventId(authToken, event.id)

                    val validRequirements = requirements.filter { it.event_id == event.id }
                    Log.d(TAG, "Found ${validRequirements.size} requirements for event: ${event.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking requirements", e)
                }
            }
        }
    }

    private fun navigateToRequirements(event: EventResponse) {
        Intent(this@HomeActivity, RequirementActivity::class.java).also { intent ->
            intent.putExtra("event_id", event.id)
            intent.putExtra("event_title", event.title)
            startActivity(intent)
        }
    }

    private fun fetchEvents() {
        if (!isNetworkAvailable) {
            showNetworkError()
            return
        }

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

                    // Check scroll indicator after events are loaded
                    binding.nestedScrollView.post {
                        checkScrollIndicatorVisibility()
                    }
                }
            } catch (e: Exception) {
                logError("Failed to fetch events", e)
                showToast("Failed to load events: ${e.message}")
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                handleLogout()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun handleLogout() {
        if (!isNetworkAvailable) {
            showToast(getString(R.string.no_internet_connection))
            performLocalLogout() // Fallback to local logout if no network
            return
        }

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
        try {
            // Create a custom dialog for the profile menu
            val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setView(R.layout.dialog_profile_menu)
                .create()

            dialog.show()

            // Set up the menu items
            val viewProfileItem = dialog.findViewById<View>(R.id.menu_view_profile)
            val logoutItem = dialog.findViewById<View>(R.id.menu_logout)

            // Set click listeners
            viewProfileItem?.setOnClickListener {
                dialog.dismiss()
                navigateToProfile()
            }

            logoutItem?.setOnClickListener {
                dialog.dismiss()
                showLogoutConfirmationDialog()
            }

        } catch (e: Exception) {
            logError("Error showing profile menu", e)
            // Fallback to standard popup menu if custom implementation fails
            PopupMenu(this, view).apply {
                menuInflater.inflate(R.menu.profile_menu, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_view_profile -> {
                            navigateToProfile()
                            true
                        }
                        R.id.action_logout -> {
                            showLogoutConfirmationDialog()
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
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
        if (!isNetworkAvailable) {
            // Load default profile icon if no network
            Glide.with(this@HomeActivity)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImageView)
            return
        }

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
        if (isNetworkAvailable) {
            fetchEvents()
            setupUnreadNotificationCount()
            setupUnreadAnnouncementCount()
            refreshProfilePicture()
        }
        startPeriodicRefresh() // Restart periodic updates
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel() // Stop updates when activity is not visible
        networkJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshJob?.cancel()
        networkJob?.cancel()

        // Unregister network callback
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering network callback", e)
            }
        }
    }
}