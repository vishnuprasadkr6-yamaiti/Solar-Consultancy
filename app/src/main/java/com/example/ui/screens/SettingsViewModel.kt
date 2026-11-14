package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsEntity
import com.example.data.model.DatabaseStats
import com.example.data.model.DefaultStages
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: SettingsEntity = SettingsEntity(),
    val stages: List<String> = DefaultStages.STAGES,
    val dbStats: DatabaseStats = DatabaseStats(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class SettingsViewModel(private val repository: SolarRepository) : ViewModel() {

    private val dbStatsFlow = MutableStateFlow(DatabaseStats())
    private val transientState = MutableStateFlow(TransientState())

    private data class TransientState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val snackbarMessage: String? = null
    )

    init {
        refreshDatabaseStats()
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settings,
        dbStatsFlow,
        transientState
    ) { settings, stats, transient ->
        SettingsUiState(
            isLoading = false,
            settings = settings,
            stages = DefaultStages.decodeStages(settings.stagesEncoded),
            dbStats = stats,
            isSaving = transient.isSaving,
            errorMessage = transient.errorMessage,
            snackbarMessage = transient.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun refreshDatabaseStats() {
        viewModelScope.launch {
            dbStatsFlow.value = repository.getDatabaseStats()
        }
    }

    fun clearError() {
        transientState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        transientState.update { it.copy(snackbarMessage = null) }
    }

    fun saveSettings(
        businessName: String,
        businessPhone: String,
        businessEmail: String,
        businessAddress: String,
        taxOrGstId: String,
        currencySymbol: String,
        currencyCode: String,
        stages: List<String>
    ) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.saveSettings(
                businessName = businessName,
                businessPhone = businessPhone,
                businessEmail = businessEmail,
                businessAddress = businessAddress,
                taxOrGstId = taxOrGstId,
                currencySymbol = currencySymbol,
                currencyCode = currencyCode,
                stages = stages
            ).fold(
                onSuccess = {
                    refreshDatabaseStats()
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "Settings saved locally.")
                    }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Failed to save settings.")
                    }
                }
            )
        }
    }

    fun resetAllData(onCompleted: () -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.resetAllApplicationData().fold(
                onSuccess = {
                    refreshDatabaseStats()
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "All local application data cleared.")
                    }
                    onCompleted()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Reset failed.")
                    }
                }
            )
        }
    }
}
