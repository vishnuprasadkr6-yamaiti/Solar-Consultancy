package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectEntity
import com.example.data.model.CustomerFilterOption
import com.example.data.model.CustomerSortOption
import com.example.data.model.ProjectStatus
import com.example.data.repository.CustomerInput
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerListItem(
    val customer: CustomerEntity,
    val totalProjects: Int,
    val activeProjects: Int,
    val completedProjects: Int,
    val totalCapacityKw: Double,
    val pendingDocumentCount: Int = 0, // Populated in Milestone 2
    val primaryStatusLabel: String
)

data class CustomersUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterOption: CustomerFilterOption = CustomerFilterOption.ACTIVE,
    val sortOption: CustomerSortOption = CustomerSortOption.RECENTLY_UPDATED,
    val customers: List<CustomerListItem> = emptyList(),
    val totalActiveCount: Int = 0,
    val totalArchivedCount: Int = 0,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class CustomersViewModel(private val repository: SolarRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterOption = MutableStateFlow(CustomerFilterOption.ACTIVE)
    private val sortOption = MutableStateFlow(CustomerSortOption.RECENTLY_UPDATED)
    private val transientState = MutableStateFlow(TransientActionState())

    private data class TransientActionState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val snackbarMessage: String? = null
    )

    val uiState: StateFlow<CustomersUiState> = combine(
        repository.allCustomers,
        repository.allProjects,
        searchQuery,
        filterOption,
        sortOption
    ) { customers, projects, query, filter, sort ->
        val projectsByCustomer = projects.groupBy { it.customerId }
        val activeCount = customers.count { !it.isArchived }
        val archivedCount = customers.count { it.isArchived }

        val mapped = customers.map { customer ->
            val custProjects = projectsByCustomer[customer.id].orEmpty()
            val activePrj = custProjects.count { it.status == ProjectStatus.ACTIVE.name }
            val completedPrj = custProjects.count { it.status == ProjectStatus.COMPLETED.name }
            val statusLabel = when {
                customer.isArchived -> "Archived"
                activePrj > 0 -> "$activePrj Active"
                completedPrj > 0 -> "$completedPrj Completed"
                custProjects.isEmpty() -> "No Projects"
                else -> "Archived Projects"
            }
            CustomerListItem(
                customer = customer,
                totalProjects = custProjects.size,
                activeProjects = activePrj,
                completedProjects = completedPrj,
                totalCapacityKw = custProjects
                    .filter { it.status != ProjectStatus.ARCHIVED.name }
                    .sumOf { it.capacityKw },
                pendingDocumentCount = 0,
                primaryStatusLabel = statusLabel
            )
        }

        val filteredByStatus = when (filter) {
            CustomerFilterOption.ACTIVE -> mapped.filter { !it.customer.isArchived }
            CustomerFilterOption.ARCHIVED -> mapped.filter { it.customer.isArchived }
            CustomerFilterOption.ALL -> mapped
        }

        val trimmedQuery = query.trim().lowercase()
        val searched = if (trimmedQuery.isEmpty()) {
            filteredByStatus
        } else {
            filteredByStatus.filter { item ->
                val c = item.customer
                c.fullName.lowercase().contains(trimmedQuery) ||
                    c.id.lowercase().contains(trimmedQuery) ||
                    c.primaryPhone.lowercase().contains(trimmedQuery) ||
                    c.secondaryPhone.lowercase().contains(trimmedQuery) ||
                    c.city.lowercase().contains(trimmedQuery)
            }
        }

        val sorted = when (sort) {
            CustomerSortOption.RECENTLY_UPDATED -> searched.sortedByDescending { it.customer.updatedAt }
            CustomerSortOption.NAME_ASC -> searched.sortedBy { it.customer.fullName.lowercase() }
            CustomerSortOption.CAPACITY_DESC -> searched.sortedByDescending { it.totalCapacityKw }
            CustomerSortOption.NEWEST -> searched.sortedByDescending { it.customer.createdAt }
        }

        CustomersUiState(
            isLoading = false,
            searchQuery = query,
            filterOption = filter,
            sortOption = sort,
            customers = sorted,
            totalActiveCount = activeCount,
            totalArchivedCount = archivedCount
        )
    }.combine(transientState) { base, transient ->
        base.copy(
            isSaving = transient.isSaving,
            errorMessage = transient.errorMessage,
            snackbarMessage = transient.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomersUiState()
    )

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun onFilterChange(option: CustomerFilterOption) {
        filterOption.value = option
    }

    fun onSortChange(option: CustomerSortOption) {
        sortOption.value = option
    }

    fun clearError() {
        transientState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        transientState.update { it.copy(snackbarMessage = null) }
    }

    fun createCustomer(input: CustomerInput, onCreated: (String) -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = repository.createCustomer(input)
            result.fold(
                onSuccess = { created ->
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = "Customer ${created.id} saved."
                        )
                    }
                    onCreated(created.id)
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = err.message ?: "Unable to save customer."
                        )
                    }
                }
            )
        }
    }

    fun updateCustomer(customerId: String, input: CustomerInput, onUpdated: () -> Unit) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = repository.updateCustomer(customerId, input)
            result.fold(
                onSuccess = { updated ->
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = "Customer ${updated.id} updated."
                        )
                    }
                    onUpdated()
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = err.message ?: "Unable to update customer."
                        )
                    }
                }
            )
        }
    }

    fun setArchived(customerId: String, archived: Boolean) {
        if (transientState.value.isSaving) return
        viewModelScope.launch {
            transientState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = repository.setCustomerArchived(customerId, archived)
            result.fold(
                onSuccess = {
                    val label = if (archived) "archived" else "restored"
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = "Customer $customerId $label."
                        )
                    }
                },
                onFailure = { err ->
                    transientState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = err.message ?: "Action failed."
                        )
                    }
                }
            )
        }
    }
}
