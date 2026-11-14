package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DashboardPreferences
import com.example.data.model.DashboardProjectFilter
import com.example.data.model.DashboardSectionId
import com.example.data.model.DashboardTimeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dashboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "solardesk_dashboard_prefs"
)

class PreferencesStore(private val context: Context) {

    private object Keys {
        val SECTION_ORDER = stringPreferencesKey("section_order")
        val HIDDEN_SECTIONS = stringPreferencesKey("hidden_sections")
        val PROJECT_FILTER = stringPreferencesKey("project_filter")
        val TIME_FILTER = stringPreferencesKey("time_filter")
    }

    val dashboardPreferences: Flow<DashboardPreferences> = context.dashboardDataStore.data.map { prefs ->
        val rawOrder = prefs[Keys.SECTION_ORDER].orEmpty()
        val parsedOrder = rawOrder.split(",")
            .mapNotNull { token -> DashboardSectionId.entries.find { it.name == token } }
        val finalOrder = (parsedOrder + DashboardSectionId.entries).distinct()

        val rawHidden = prefs[Keys.HIDDEN_SECTIONS].orEmpty()
        val hidden = rawHidden.split(",")
            .mapNotNull { token -> DashboardSectionId.entries.find { it.name == token } }
            .toSet()

        val projectFilter = prefs[Keys.PROJECT_FILTER]
            ?.let { token -> DashboardProjectFilter.entries.find { it.name == token } }
            ?: DashboardProjectFilter.ACTIVE

        val timeFilter = prefs[Keys.TIME_FILTER]
            ?.let { token -> DashboardTimeFilter.entries.find { it.name == token } }
            ?: DashboardTimeFilter.ALL_TIME

        DashboardPreferences(
            sectionOrder = finalOrder,
            hiddenSections = hidden,
            projectFilter = projectFilter,
            timeFilter = timeFilter
        )
    }

    suspend fun saveProjectFilter(filter: DashboardProjectFilter) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.PROJECT_FILTER] = filter.name
        }
    }

    suspend fun saveTimeFilter(filter: DashboardTimeFilter) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.TIME_FILTER] = filter.name
        }
    }

    suspend fun toggleSectionVisibility(sectionId: DashboardSectionId) {
        context.dashboardDataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_SECTIONS].orEmpty()
                .split(",")
                .mapNotNull { token -> DashboardSectionId.entries.find { it.name == token } }
                .toMutableSet()
            if (current.contains(sectionId)) {
                current.remove(sectionId)
            } else {
                current.add(sectionId)
            }
            prefs[Keys.HIDDEN_SECTIONS] = current.joinToString(",") { it.name }
        }
    }

    suspend fun saveSectionOrder(order: List<DashboardSectionId>) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.SECTION_ORDER] = order.joinToString(",") { it.name }
        }
    }

    suspend fun resetDashboardPreferences() {
        context.dashboardDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
