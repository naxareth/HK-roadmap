package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.second_year.hkroadmap.Adapters.AnnouncementAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.databinding.ActivityAnnouncementBinding
import kotlinx.coroutines.launch

class AnnouncementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnouncementBinding
    private lateinit var announcementAdapter: AnnouncementAdapter
    private val TAG = "AnnouncementActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        fetchAnnouncements()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Announcements"
        }
    }

    private fun setupRecyclerView() {
        announcementAdapter = AnnouncementAdapter()
        binding.recyclerView.apply {
            adapter = announcementAdapter
            layoutManager = LinearLayoutManager(this@AnnouncementActivity)
        }
    }

    private fun fetchAnnouncements() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showToast("Authentication error")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.createApiService()
                    .getStudentAnnouncements("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.announcements
                    announcementAdapter.submitList(announcements)
                    binding.recyclerView.visibility = View.VISIBLE
                } else {
                    showToast("Failed to fetch announcements")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching announcements", e)
                showToast("Error loading announcements")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onResume() {
        super.onResume()
        fetchAnnouncements()
    }
}