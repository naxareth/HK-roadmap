package com.second_year.hkroadmap.Views

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.second_year.hkroadmap.Adapters.NotificationAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Repository.NotificationRepository
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.ViewModel.NotificationViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityNotificationBinding
import android.util.Log
class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.markAllAsRead()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Notifications"
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            viewModel.toggleNotificationRead(notification.notification_id, true)
            viewModel.fetchUnreadCount()
            setResult(RESULT_OK)
        }

        binding.notificationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = this@NotificationActivity.adapter
        }
    }

    private fun setupViewModel() {
        val token = TokenManager.getToken(this) ?: run {
            finish()
            return
        }

        val apiService = RetrofitInstance.createApiService()
        val repository = NotificationRepository(apiService)
        val factory = ViewModelFactory(
            notificationRepository = repository,
            token = token
        )

        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchNotifications()
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(this) { notifications ->
            binding.swipeRefresh.isRefreshing = false

            try {
                Log.d("NotificationActivity", "Received notifications: $notifications")

                if (notifications.isEmpty()) {
                    Log.d("NotificationActivity", "Notifications list is empty")
                    binding.emptyView.visibility = View.VISIBLE
                    binding.notificationRecyclerView.visibility = View.GONE
                } else {
                    Log.d("NotificationActivity", "Showing ${notifications.size} notifications")
                    binding.emptyView.visibility = View.GONE
                    binding.notificationRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(notifications)
                }
            } catch (e: Exception) {
                Log.e("NotificationActivity", "Error handling notifications", e)
            }
        }

        viewModel.markReadStatus.observe(this) { response ->
            Log.d("NotificationActivity", "Mark read response: $response")
            response?.let {
                if (it.success) {
                    viewModel.fetchUnreadCount()
                    setResult(RESULT_OK)
                }
            }
        }

        viewModel.unreadCount.observe(this) { response ->
            Log.d("NotificationActivity", "Unread count response: $response")
            response?.let {
                Log.d("NotificationActivity", "Unread count: ${it.unread_count}")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_mark_all_read -> {
                viewModel.markAllAsRead()
                setResult(RESULT_OK)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchNotifications()
        viewModel.fetchUnreadCount()
    }
}