package com.second_year.hkroadmap

import android.app.Application
import com.google.android.gms.ads.MobileAds

class HKRoadmapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) {}
    }
}