package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ChecklistProjectStatusRow
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsEntity
import com.example.data.model.DashboardPreferences
import com.example.data.model.DashboardProjectFilter
import com.example.data.model.DashboardSectionId
import com.example.data.model.DashboardTimeFilter
import com.example.data.model.DefaultStages
import com.example.data.model.ProjectStatus
import com.example.data.repository.SolarRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StageCount(
    val stageName: String,
    val count: Int,
    val capacityKw: Double
)

data class ProjectSummaryRow(
    val project: ProjectEntity,
    val customerName: String,
    val attentionReason: String? = null
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val businessName: String = "Solar Consultancy",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val totalCustomers: Int = 0,
    val activeProjects: Int = 0,
    val completedProjects: Int = 0,
    val checklistCompletedProjects: Int = 0,
    val checklistPendingProjects: Int = 0,
    val totalCapacityKw: Double = 0.0,
    val quotedPipelineAmount: Double = 0.0,
    val stageBreakdown: List<StageCount> = emptyList(),
    val projectsNeedingAttention: List<ProjectSummaryRow> = emptyList(),
    val recentlyUpdatedProjects: List<ProjectSummaryRow> = emptyList(),
    val recentActivity: List<ActivityLogEntity> = emptyList(),
    val preferences: DashboardPreferences = DashboardPreferences()
)

class DashboardViewModel(private val repository: SolarRepository) : ViewModel() {

    private data class CoreDashboardBundle(
        val customers: List<CustomerEntity>,
        val projects: List<ProjectEntity>,
        val checklistSummary: List<ChecklistProjectStatusRow>
    )

    private val coreBundle = combine(
        repository.allCustomers,
        repository.allProjects,
        repository.checklistCompletionSummary
    ) { customers, projects, checklistSummary ->
        CoreDashboardBundle(
            customers = customers,
            projects = projects,
            checklistSummary = checklistSummary
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        coreBundle,
        repository.recentActivity,
        repository.settings,
        repository.dashboardPreferences
    ) { core: CoreDashboardBundle,
        activities: List<ActivityLogEntity>,
        settings: SettingsEntity,
        prefs: DashboardPreferences ->

        val customers = core.customers
        val projects = core.projects
        val customerMap = customers.associateBy { it.id }
        val activeCustomers = customers.count { !it.isArchived }

        val activeCount = projects.count { it.status == ProjectStatus.ACTIVE.name }
        val completedCount = projects.count { it.status == ProjectStatus.COMPLETED.name }

        val checklistCompletedCount = core.checklistSummary.count {
            it.totalItems > 0 && it.completedItems >= it.totalItems
        }
        val checklistPendingCount = core.checklistSummary.count {
            it.totalItems == 0 || it.completedItems < it.totalItems
        }

        val filteredProjects = when (prefs.projectFilter) {
            DashboardProjectFilter.ACTIVE -> projects.filter { it.status == ProjectStatus.ACTIVE.name }
            DashboardProjectFilter.COMPLETED -> projects.filter { it.status == ProjectStatus.COMPLETED.name }
            DashboardProjectFilter.ALL -> projects.filter { it.status != ProjectStatus.ARCHIVED.name }
        }

        val monthStart = Formatters.startOfCurrentMonthMillis()
        val timeFilteredProjects = when (prefs.timeFilter) {
            DashboardTimeFilter.CURRENT_MONTH -> filteredProjects.filter { it.updatedAt >= monthStart || it.createdAt >= monthStart }
            DashboardTimeFilter.ALL_TIME -> filteredProjects
        }

        val configuredStages = DefaultStages.decodeStages(settings.stagesEncoded)
        val stageBreakdown = configuredStages.map { stage ->
            val matching = filteredProjects.filter { it.currentStage.equals(stage, ignoreCase = true) }
            StageCount(
                stageName = stage,
                count = matching.size,
                capacityKw = matching.sumOf { it.capacityKw }
            )
        }

        val now = System.currentTimeMillis()
        val staleThresholdMs = 14L * 24L * 60L * 60L * 1000L // 14 days without update

        val attentionRows = projects
            .filter { it.status == ProjectStatus.ACTIVE.name }
            .mapNotNull { project ->
                val reason = when {
                    project.expectedCompletionDate != null && project.expectedCompletionDate < now ->
                        "Past expected completion (${Formatters.formatDate(project.expectedCompletionDate)})"
                    project.updatedAt < now - staleThresholdMs ->
                        "No update in 14+ days"
                    project.quotationAmount <= 0.0 &&
                        !project.currentStage.equals("Work Order Signing", ignoreCase = true) ->
                        "Quotation amount not entered"
                    else -> null
                }
                reason?.let {
                    ProjectSummaryRow(
                        project = project,
                        customerName = customerMap[project.customerId]?.fullName ?: project.customerId,
                        attentionReason = it
                    )
                }
            }
            .take(8)

        val recentProjectRows = filteredProjects
            .sortedByDescending { it.updatedAt }
            .take(8)
            .map { project ->
                ProjectSummaryRow(
                    project = project,
                    customerName = customerMap[project.customerId]?.fullName ?: project.customerId
                )
            }

        DashboardUiState(
            isLoading = false,
            businessName = settings.businessName,
            currencySymbol = settings.currencySymbol,
            currencyCode = settings.currencyCode,
            totalCustomers = activeCustomers,
            activeProjects = activeCount,
            completedProjects = completedCount,
            checklistCompletedProjects = checklistCompletedCount,
            checklistPendingProjects = checklistPendingCount,
            totalCapacityKw = filteredProjects.sumOf { it.capacityKw },
            quotedPipelineAmount = timeFilteredProjects.sumOf { it.quotationAmount },
            stageBreakdown = stageBreakdown,
            projectsNeedingAttention = attentionRows,
            recentlyUpdatedProjects = recentProjectRows,
            recentActivity = activities.take(12),
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setProjectFilter(filter: DashboardProjectFilter) {
        viewModelScope.launch { repository.setDashboardProjectFilter(filter) }
    }

    fun setTimeFilter(filter: DashboardTimeFilter) {
        viewModelScope.launch { repository.setDashboardTimeFilter(filter) }
    }

    fun toggleSection(sectionId: DashboardSectionId) {
        viewModelScope.launch { repository.toggleDashboardSection(sectionId) }
    }

    fun moveSection(sectionId: DashboardSectionId, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveDashboardSection(
                sectionId = sectionId,
                moveUp = moveUp,
                currentOrder = uiState.value.preferences.sectionOrder
            )
        }
    }
}
