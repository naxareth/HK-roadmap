package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.second_year.hkroadmap.Adapters.RequirementsAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityRequirementBinding
import com.second_year.hkroadmap.databinding.ItemEventBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RequirementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementBinding
    private lateinit var requirementsAdapter: RequirementsAdapter
    private var eventId: Int = -1
    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventLocation: String = ""
    private val TAG = "RequirementActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get event details from intent
        eventId = intent.getIntExtra("event_id", -1)
        eventTitle = intent.getStringExtra("event_title") ?: "Requirements"
        eventDate = intent.getStringExtra("event_date") ?: ""
        eventLocation = intent.getStringExtra("event_location") ?: "TBD"

        Log.d(TAG, "Received event details - ID: $eventId, Title: $eventTitle, Date: $eventDate, Location: $eventLocation")

        if (eventId == -1) {
            showToast("Invalid event")
            finish()
            return
        }

        setupToolbar()
        setupEventDetails()
        setupRecyclerView()
        fetchRequirements()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = eventTitle
        }
    }

    private fun setupEventDetails() {
        try {
            val eventCardView = layoutInflater.inflate(R.layout.item_event, binding.eventContainer, false)
            binding.eventContainer.addView(eventCardView)
            val eventBinding = ItemEventBinding.bind(eventCardView)

            eventBinding.apply {
                tvEventTitle.text = eventTitle
                tvEventLocation.text = eventLocation

                try {
                    if (eventDate == "0000-00-00 00:00:00" || eventDate.isEmpty()) {
                        tvEventDate.text = "Date TBD"
                    } else {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        val date = inputFormat.parse(eventDate)
                        if (date != null) {
                            tvEventDate.text = outputFormat.format(date)
                        } else {
                            tvEventDate.text = "Date unavailable"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date: $eventDate", e)
                    tvEventDate.text = "Date unavailable"
                }

                // Disable click events on the event card
                root.isClickable = false
                root.isFocusable = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up event details", e)
        }
    }

    private fun setupRecyclerView() {
        requirementsAdapter = RequirementsAdapter()
        binding.requirementsRecyclerView.apply {
            adapter = requirementsAdapter
            layoutManager = LinearLayoutManager(this@RequirementActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun fetchRequirements() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showToast("Authentication error")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val authToken = "Bearer $token"
                val requirements = RetrofitInstance.createApiService()
                    .getRequirementsByEventId(authToken, eventId)

                // Filter requirements for this specific event
                val validRequirements = requirements.filter { it.event_id == eventId }

                if (validRequirements.isEmpty()) {
                    showToast("No requirements found for this event")
                } else {
                    requirementsAdapter.setRequirements(validRequirements)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch requirements", e)
                showToast("Failed to load requirements: ${e.message}")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}