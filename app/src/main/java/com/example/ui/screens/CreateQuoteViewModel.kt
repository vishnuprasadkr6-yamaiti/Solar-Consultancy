package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsEntity
import com.example.data.repository.SolarRepository
import com.example.util.GeneratedQuoteResult
import com.example.util.QuoteInputData
import com.example.util.QuotePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateQuoteUiState(
    val isLoading: Boolean = true,
    val project: ProjectEntity? = null,
    val customer: CustomerEntity? = null,
    val settings: SettingsEntity = SettingsEntity(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val generatedResult: GeneratedQuoteResult? = null
)

class CreateQuoteViewModel(
    private val projectId: String,
    private val repository: SolarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuoteUiState())
    val uiState: StateFlow<CreateQuoteUiState> = _uiState.asStateFlow()

    init {
        loadProjectContext()
    }

    private fun loadProjectContext() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            val customer = project?.let { repository.getCustomerById(it.customerId) }
            val settings = repository.getSettingsSnapshot()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    project = project,
                    customer = customer,
                    settings = settings
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearGeneratedResult() {
        _uiState.update { it.copy(generatedResult = null) }
    }

    fun generateQuotePdf(
        context: Context,
        input: QuoteInputData,
        updateProjectQuotation: Boolean
    ) {
        val state = _uiState.value
        val project = state.project
        val customer = state.customer
        if (project == null || customer == null || state.isGenerating) return

        if (input.systemPrice <= 0.0) {
            _uiState.update { it.copy(errorMessage = "System Price must be greater than ₹0.") }
            return
        }
        if (input.deliveryPeriod.isBlank() || input.installationPeriod.isBlank() || input.netMeterPeriod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter Delivery, Installation, and Net Meter periods.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            val result = runCatching {
                val pdfResult = withContext(Dispatchers.IO) {
                    QuotePdfGenerator.generateQuotePdf(
                        context = context.applicationContext,
                        settings = state.settings,
                        customer = customer,
                        project = project,
                        quote = input
                    )
                }
                repository.recordQuoteGenerated(
                    projectId = project.id,
                    totalQuoteAmount = pdfResult.grandTotal,
                    updateProjectQuotation = updateProjectQuotation
                )
                pdfResult
            }

            result.fold(
                onSuccess = { generated ->
                    _uiState.update {
                        it.copy(isGenerating = false, generatedResult = generated)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = err.message ?: "Failed to generate PDF."
                        )
                    }
                }
            )
        }
    }
}
