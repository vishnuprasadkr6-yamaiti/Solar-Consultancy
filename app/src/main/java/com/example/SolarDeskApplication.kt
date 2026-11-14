package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesStore
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SolarDeskApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesStore: PreferencesStore by lazy { PreferencesStore(this) }
    val repository: SolarRepository by lazy {
        SolarRepository(
            context = applicationContext,
            db = database,
            preferencesStore = preferencesStore
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.ensureDefaultSettings()
        }
    }
}
