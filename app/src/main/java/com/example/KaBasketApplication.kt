package com.example

import android.app.Application
import com.example.supabase.SupabaseSyncManager
import com.example.vision.VideoAnalysisNotificationHelper

class KaBasketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoAnalysisNotificationHelper.createNotificationChannel(this)
        com.example.stats.PlayerStatsManager.init(this)
        com.example.supabase.SupabaseConfig.init(this)
        SupabaseSyncManager.init(this)
        com.example.vision.VoiceCoachManager.init(this)
    }
}
