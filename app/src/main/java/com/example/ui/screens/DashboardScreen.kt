package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ActivityLogEntity
import com.example.data.model.DashboardProjectFilter
import com.example.data.model.DashboardSectionId
import com.example.data.model.DashboardTimeFilter
import com.example.data.model.ProjectStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IdPill
import com.example.ui.components.LocalOnlyNoticeBanner
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.SolarCard
import com.example.ui.components.WarningPill
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenCustomer: (String) -> Unit,
    onNavigateToCustomers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomizeSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 840.dp
        val maxContentWidth = if (isWide) 1120.dp else 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.businessName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Operations Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        OutlinedButton(
                            onClick = { showCustomizeSheet = true },
                            modifier = Modifier.testTag("customize_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Customize dashboard",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Customize")
                        }
                    }

                    // Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DashboardProjectFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.preferences.projectFilter == filter,
                                onClick = { viewModel.setProjectFilter(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("dashboard_project_filter_${filter.name.lowercase()}")
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        DashboardTimeFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.preferences.timeFilter == filter,
                                onClick = { viewModel.setTimeFilter(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("dashboard_time_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    // Local-first reminder
                    LocalOnlyNoticeBanner(compact = true)

                    // Real-time Summary KPI Grid
                    val columnCount = if (isWide) 4 else 2
                    val kpiCards = listOf(
                        KpiSpec(
                            title = "Total Customers",
                            value = state.totalCustomers.toString(),
                            subtitle = "Active profiles",
                            icon = Icons.Outlined.People,
                            accent = true
                        ),
                        KpiSpec(
                            title = "Active Projects",
                            value = state.activeProjects.toString(),
                            subtitle = Formatters.formatCapacityKw(state.totalCapacityKw) + " filtered",
                            icon = Icons.Outlined.Bolt,
                            accent = true
                        ),
                        KpiSpec(
                            title = "Completed Projects",
                            value = state.completedProjects.toString(),
                            subtitle = "Commissioned",
                            icon = Icons.Outlined.CheckCircle
                        ),
                        KpiSpec(
                            title = "Quoted Pipeline",
                            value = Formatters.formatCurrency(
                                state.quotedPipelineAmount,
                                state.currencySymbol,
                                state.currencyCode
                            ),
                            subtitle = state.preferences.timeFilter.label,
                            icon = Icons.Outlined.AccountBalanceWallet
                        )
                    )

                    kpiCards.chunked(columnCount).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { spec ->
                                MetricSummaryCard(
                                    title = spec.title,
                                    value = spec.value,
                                    subtitle = spec.subtitle,
                                    icon = spec.icon,
                                    highlightAccent = spec.accent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columnCount - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // First-launch callout when database has zero customers
                    if (!state.isLoading && state.totalCustomers == 0 && state.activeProjects == 0) {
                        EmptyStateCard(
                            icon = Icons.Outlined.FolderOpen,
                            title = "Ready for your first solar customer",
                            description = "Add a customer and create one or more solar projects. All dashboard metrics, pipeline stage counts, and activity logs update automatically from your local database.",
                            action = {
                                Button(
                                    onClick = onNavigateToCustomers,
                                    modifier = Modifier.testTag("dashboard_add_first_customer_button")
                                ) {
                                    Text("Go to Customers")
                                }
                            }
                        )
                    }
                }
            }

            // Customizable Sections in user-configured order
            val visibleSections = state.preferences.sectionOrder.filter {
                !state.preferences.hiddenSections.contains(it)
            }

            visibleSections.forEach { sectionId ->
                item(key = sectionId.name) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth()
                    ) {
                        when (sectionId) {
                            DashboardSectionId.STAGE_OVERVIEW -> {
                                StageOverviewSection(
                                    stages = state.stageBreakdown,
                                    filterLabel = state.preferences.projectFilter.label
                                )
                            }

                            DashboardSectionId.PROJECTS_ATTENTION -> {
                                ProjectsAttentionSection(
                                    rows = state.projectsNeedingAttention,
                                    onOpenCustomer = onOpenCustomer
                                )
                            }

                            DashboardSectionId.RECENT_PROJECTS -> {
                                RecentProjectsSection(
                                    rows = state.recentlyUpdatedProjects,
                                    currencySymbol = state.currencySymbol,
                                    currencyCode = state.currencyCode,
                                    onOpenCustomer = onOpenCustomer
                                )
                            }

                            DashboardSectionId.RECENT_ACTIVITY -> {
                                RecentActivitySection(activities = state.recentActivity)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customize Dashboard Sections",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Show, hide, or reorder sections. Preferences are saved locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val order = state.preferences.sectionOrder
                order.forEachIndexed { index, sectionId ->
                    val isVisible = !state.preferences.hiddenSections.contains(sectionId)
                    SolarCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sectionId.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = sectionId.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveSection(sectionId, moveUp = true) },
                                enabled = index > 0,
                                modifier = Modifier.testTag("move_up_${sectionId.name.lowercase()}")
                            ) {
                                Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = { viewModel.moveSection(sectionId, moveUp = false) },
                                enabled = index < order.lastIndex,
                                modifier = Modifier.testTag("move_down_${sectionId.name.lowercase()}")
                            ) {
                                Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down")
                            }
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { viewModel.toggleSection(sectionId) },
                                modifier = Modifier.testTag("toggle_${sectionId.name.lowercase()}")
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { showCustomizeSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

private data class KpiSpec(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Boolean = false
)

@Composable
private fun StageOverviewSection(
    stages: List<StageCount>,
    filterLabel: String
) {
    val totalProjects = stages.sumOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Project Overview by Stage",
            subtitle = "Showing $filterLabel"
        )
        SolarCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stages.forEachIndexed { index, stage ->
                    val fraction = stage.count.toFloat() / totalProjects.toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${stage.stageName}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (stage.count > 0) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (stage.count > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (stage.count > 0) {
                                    "${stage.count} (${Formatters.formatCapacityKw(stage.capacityKw)})"
                                } else {
                                    "0"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (stage.count > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (stages.sumOf { it.count } == 0) 0f else fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsAttentionSection(
    rows: List<ProjectSummaryRow>,
    onOpenCustomer: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Projects Needing Attention",
            subtitle = "Overdue target dates, stale updates, or missing quotations"
        )
        if (rows.isEmpty()) {
            SolarCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "All clear",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No active projects flagged for attention right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            SolarCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    rows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCustomer(row.project.customerId) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IdPill(row.project.id)
                                    Text(
                                        text = row.project.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${row.customerName} • ${row.project.currentStage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                row.attentionReason?.let { reason ->
                                    WarningPill(text = reason)
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Needs attention",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (index < rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentProjectsSection(
    rows: List<ProjectSummaryRow>,
    currencySymbol: String,
    currencyCode: String,
    onOpenCustomer: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Recently Updated Projects",
            subtitle = "Tap any project to open its customer workspace"
        )
        if (rows.isEmpty()) {
            SolarCard {
                Text(
                    text = "No matching projects yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            SolarCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    rows.forEachIndexed { index, row ->
                        val status = runCatching { ProjectStatus.valueOf(row.project.status) }
                            .getOrDefault(ProjectStatus.ACTIVE)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCustomer(row.project.customerId) }
                                .padding(14.dp)
                                .testTag("dashboard_project_row_${row.project.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IdPill(row.project.id)
                                    Text(
                                        text = row.project.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${row.customerName} • ${Formatters.formatCapacityKw(row.project.capacityKw)} • ${row.project.currentStage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Updated ${Formatters.formatDate(row.project.updatedAt)} • ${
                                        Formatters.formatCurrency(
                                            row.project.quotationAmount,
                                            currencySymbol,
                                            currencyCode
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            ProjectStatusBadge(status = status)
                        }
                        if (index < rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivitySection(activities: List<ActivityLogEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Recent Activity",
            subtitle = "Recorded local changes and stage updates"
        )
        if (activities.isEmpty()) {
            SolarCard {
                Text(
                    text = "Activity log will appear here as you add customers, projects, and stage updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            SolarCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    activities.forEachIndexed { index, activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Activity",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activity.id} • ${Formatters.formatDateTime(activity.timestamp)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < activities.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
