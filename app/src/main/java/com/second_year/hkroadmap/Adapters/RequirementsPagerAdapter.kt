package com.second_year.hkroadmap.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.second_year.hkroadmap.Fragments.AllRequirementsFragment
import com.second_year.hkroadmap.Fragments.MissingRequirementsFragment
import com.second_year.hkroadmap.Fragments.SubmittedRequirementsFragment

class RequirementsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllRequirementsFragment()
            1 -> SubmittedRequirementsFragment()
            2 -> MissingRequirementsFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}