package com.example.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectChecklist
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectPhoto
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
import java.io.File

data class CustomerDetailUiState(
    val isLoading: Boolean = true,
    val customer: CustomerEntity? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val checklistsByProject: Map<String, List<ProjectChecklist>> = emptyMap(),
    val photosByProject: Map<String, List<ProjectPhoto>> = emptyMap(),
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

    private data class CustomerCoreBundle(
        val customer: CustomerEntity?,
        val projects: List<ProjectEntity>,
        val checklists: List<ProjectChecklist>,
        val photos: List<ProjectPhoto>
    )

    init {
        viewModelScope.launch {
            repository.ensureProjectChecklistsSeeded(customerId)
        }
    }

    private val coreFlow = combine(
        repository.observeCustomer(customerId),
        repository.observeCustomerProjects(customerId),
        repository.observeCustomerChecklists(customerId),
        repository.observeCustomerPhotos(customerId)
    ) { customer, projects, checklists, photos ->
        CustomerCoreBundle(
            customer = customer,
            projects = projects,
            checklists = checklists,
            photos = photos
        )
    }

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        coreFlow,
        repository.observeCustomerActivity(customerId),
        repository.settings,
        transientState
    ) { core, activities, settings, transient ->
        val stages = DefaultStages.decodeStages(settings.stagesEncoded)
        val nonArchived = core.projects.filter { it.status != ProjectStatus.ARCHIVED.name }
        CustomerDetailUiState(
            isLoading = false,
            customer = core.customer,
            projects = core.projects,
            checklistsByProject = core.checklists.groupBy { it.projectId },
            photosByProject = core.photos.groupBy { it.projectId },
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

    fun showTransientMessage(message: String) {
        transientState.update { it.copy(snackbarMessage = message) }
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

    fun toggleChecklistItem(itemId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleChecklistItem(itemId, completed).onFailure { err ->
                transientState.update {
                    it.copy(snackbarMessage = err.message ?: "Unable to update checklist step.")
                }
            }
        }
    }

    fun updateChecklistItemNote(itemId: String, note: String) {
        viewModelScope.launch {
            repository.updateChecklistItemNote(itemId, note).fold(
                onSuccess = {
                    transientState.update { it.copy(snackbarMessage = "Checklist note saved.") }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Unable to save note.")
                    }
                }
            )
        }
    }

    fun addCustomChecklistItem(projectId: String, title: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addCustomChecklistItem(projectId, title).fold(
                onSuccess = {
                    transientState.update { it.copy(snackbarMessage = "Checklist step added.") }
                    onSuccess()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Unable to add checklist step.")
                    }
                }
            )
        }
    }

    fun createPhotoCaptureTarget(projectId: String): Pair<File, Uri> {
        return repository.createPhotoOutputUri(projectId)
    }

    fun onPhotoCaptured(projectId: String, localFile: File, localUri: Uri, caption: String = "") {
        viewModelScope.launch {
            repository.saveProjectPhoto(projectId, localFile, localUri, caption).fold(
                onSuccess = {
                    transientState.update {
                        it.copy(snackbarMessage = "Site photo saved locally & attached.")
                    }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Failed to attach photo.")
                    }
                }
            )
        }
    }

    fun onGalleryPhotoPicked(projectId: String, sourceUri: Uri) {
        viewModelScope.launch {
            repository.savePickedGalleryPhoto(projectId, sourceUri).fold(
                onSuccess = {
                    transientState.update {
                        it.copy(snackbarMessage = "Gallery photo saved locally & attached.")
                    }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Failed to attach gallery photo.")
                    }
                }
            )
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            repository.deleteProjectPhoto(photoId).fold(
                onSuccess = {
                    transientState.update { it.copy(snackbarMessage = "Site photo deleted.") }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Unable to delete photo.")
                    }
                }
            )
        }
    }
}
