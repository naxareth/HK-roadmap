package com.second_year.hkroadmap

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.MobileAds

class HKRoadmapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) {}

    }
}