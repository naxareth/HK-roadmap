package com.second_year.hkroadmap.Views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.second_year.hkroadmap.Adapters.RequirementsPagerAdapter
import com.second_year.hkroadmap.Fragments.AllRequirementsFragment
import com.second_year.hkroadmap.Fragments.MissingRequirementsFragment
import com.second_year.hkroadmap.Fragments.SubmittedRequirementsFragment
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ActivityViewRequirementsBinding

class ViewRequirementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewRequirementsBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewRequirementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupViewPager() {
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout

        val pagerAdapter = RequirementsPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All"
                1 -> "Submitted"
                2 -> "Missing"
                else -> ""
            }
        }.attach()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Get current fragment and refresh its data
            val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
            when (currentFragment) {
                is AllRequirementsFragment -> currentFragment.fetchAllRequirements()
                is SubmittedRequirementsFragment -> currentFragment.fetchSubmittedRequirements()
                is MissingRequirementsFragment -> currentFragment.fetchMissingRequirements()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Customize SwipeRefreshLayout (optional)
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.primary_green) // Use your app's color
            setProgressBackgroundColorSchemeResource(android.R.color.white)
        }
    }

    // Interface to communicate with fragments
    interface RefreshableFragment {
        fun refresh()
    }
}