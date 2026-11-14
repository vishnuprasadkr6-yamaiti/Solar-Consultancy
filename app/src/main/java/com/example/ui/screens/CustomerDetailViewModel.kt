package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectEntity
import com.example.data.model.DefaultStages
import com.example.data.model.ProjectStatus
import com.example.data.repository.CustomerInput
import com.example.data.repository.ProjectInput
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerDetailUiState(
    val isLoading: Boolean = true,
    val customer: CustomerEntity? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val activities: List<ActivityLogEntity> = emptyList(),
    val availableStages: List<String> = DefaultStages.STAGES,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val activeCapacityKw: Double = 0.0,
    val totalQuotedAmount: Double = 0.0,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class CustomerDetailViewModel(
    private val customerId: String,
    private val repository: SolarRepository
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientState())

    private data class TransientState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val snackbarMessage: String? = null
    )

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        repository.observeCustomer(customerId),
        repository.observeCustomerProjects(customerId),
        repository.observeCustomerActivity(customerId),
        repository.settings,
        transientState
    ) { customer, projects, activities, settings, transient ->
        val stages = DefaultStages.decodeStages(settings.stagesEncoded)
        val nonArchived = projects.filter { it.status != ProjectStatus.ARCHIVED.name }
        CustomerDetailUiState(
            isLoading = false,
            customer = customer,
            projects = projects,
            activities = activities,
            availableStages = stages,
            currencySymbol = settings.currencySymbol,
            currencyCode = settings.currencyCode,
            activeCapacityKw = nonArchived.sumOf { it.capacityKw },
            totalQuotedAmount = nonArchived.sumOf { it.quotationAmount },
            isSaving = transient.isSaving,
            errorMessage = transient.errorMessage,
            snackbarMessage = transient.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomerDetailUiState()
    )

    fun clearError() {
        transientState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        transientState.update { it.copy(snackbarMessage = null) }
    }

    fun updateCustomer(input: CustomerInput, onSuccess: () -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateCustomer(customerId, input).fold(
                onSuccess = {
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "Customer profile updated.")
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Update failed.")
                    }
                }
            )
        }
    }

    fun toggleArchiveCustomer() {
        val current = uiState.value.customer ?: return
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.setCustomerArchived(current.id, !current.isArchived).fold(
                onSuccess = {
                    val msg = if (!current.isArchived) "Customer archived." else "Customer restored."
                    transientState.update { it.copy(isSaving = false, snackbarMessage = msg) }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Action failed.")
                    }
                }
            )
        }
    }

    fun createProject(input: ProjectInput, onSuccess: () -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.createProject(input).fold(
                onSuccess = { created ->
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "Project ${created.id} created.")
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Unable to create project.")
                    }
                }
            )
        }
    }

    fun updateProject(projectId: String, input: ProjectInput, onSuccess: () -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateProject(projectId, input).fold(
                onSuccess = { updated ->
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "Project ${updated.id} updated.")
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Unable to update project.")
                    }
                }
            )
        }
    }

    fun updateProjectStage(
        projectId: String,
        newStage: String,
        newStatus: ProjectStatus,
        note: String,
        onSuccess: () -> Unit
    ) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateProjectStage(
                projectId = projectId,
                newStage = newStage,
                newStatus = newStatus,
                stageNote = note
            ).fold(
                onSuccess = {
                    transientState.update {
                        it.copy(isSaving = false, snackbarMessage = "Stage updated to '$newStage'.")
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(isSaving = false, errorMessage = err.message ?: "Unable to update stage.")
                    }
                }
            )
        }
    }
}
