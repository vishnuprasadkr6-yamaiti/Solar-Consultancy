package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IdPill
import com.example.ui.components.ProjectFormDialog
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.SolarCard
import com.example.ui.components.StageProgressionDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusArchivedBg
import com.example.ui.theme.StatusArchivedText
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditCustomer by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var stagingProject by remember { mutableStateOf<ProjectEntity?>(null) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.customer?.fullName ?: "Customer Details",
                            style = MaterialTheme.typography.titleMedium
                        )
                        state.customer?.let {
                            Text(
                                text = it.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Customers"
                        )
                    }
                },
                actions = {
                    if (state.customer != null) {
                        IconButton(
                            onClick = {
                                viewModel.clearError()
                                showEditCustomer = true
                            },
                            modifier = Modifier.testTag("edit_customer_button")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Customer")
                        }
                        IconButton(
                            onClick = { showArchiveConfirm = true },
                            modifier = Modifier.testTag("archive_customer_button")
                        ) {
                            Icon(
                                imageVector = if (state.customer?.isArchived == true) {
                                    Icons.Outlined.Unarchive
                                } else {
                                    Icons.Outlined.Archive
                                },
                                contentDescription = if (state.customer?.isArchived == true) {
                                    "Restore Customer"
                                } else {
                                    "Archive Customer"
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val customer = state.customer
        if (!state.isLoading && customer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Outlined.Person,
                    title = "Customer not found",
                    description = "This customer record may have been cleared.",
                    action = {
                        Button(onClick = onBack) { Text("Back to Customers") }
                    }
                )
            }
            return@Scaffold
        }

        if (customer != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val maxContentWidth = if (maxWidth >= 840.dp) 1040.dp else 720.dp

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("customer_detail_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile & Summary Card
                    item {
                        Column(
                            modifier = Modifier
                                .widthIn(max = maxContentWidth)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SolarCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IdPill(customer.id)
                                            if (customer.isArchived) {
                                                StatusPill(
                                                    label = "Archived",
                                                    containerColor = StatusArchivedBg,
                                                    contentColor = StatusArchivedText
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Added ${Formatters.formatDate(customer.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = customer.fullName,
                                        style = MaterialTheme.typography.headlineSmall
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Phone,
                                            contentDescription = "Phone",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        val phones = buildString {
                                            append(customer.primaryPhone)
                                            if (customer.secondaryPhone.isNotBlank()) {
                                                append(" • Alt: ${customer.secondaryPhone}")
                                            }
                                        }
                                        Text(text = phones, style = MaterialTheme.typography.bodyMedium)
                                    }

                                    if (customer.email.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Email,
                                                contentDescription = "Email",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(text = customer.email, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }

                                    val fullAddress = listOf(
                                        customer.address,
                                        customer.city,
                                        customer.postalCode
                                    ).filter { it.isNotBlank() }.joinToString(", ")

                                    if (fullAddress.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = "Address",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(text = fullAddress, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }

                                    if (customer.notes.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = customer.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Customer KPI Summary Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SummaryMiniStat(
                                            label = "Projects",
                                            value = state.projects.size.toString(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SummaryMiniStat(
                                            label = "Total Capacity",
                                            value = Formatters.formatCapacityKw(state.activeCapacityKw),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SummaryMiniStat(
                                            label = "Quoted Value",
                                            value = Formatters.formatCurrency(
                                                state.totalQuotedAmount,
                                                state.currencySymbol,
                                                state.currencyCode
                                            ),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }
                            }

                        }
                    }

                    // Projects Header
                    item {
                        Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                            SectionHeader(
                                title = "Solar Projects (${state.projects.size})",
                                subtitle = "One customer can hold multiple independent projects",
                                action = {
                                    Button(
                                        onClick = {
                                            viewModel.clearError()
                                            showAddProject = true
                                        },
                                        modifier = Modifier.testTag("add_project_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "Add Project",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Project")
                                    }
                                }
                            )
                        }
                    }

                    if (state.projects.isEmpty()) {
                        item {
                            Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                                EmptyStateCard(
                                    icon = Icons.Outlined.Bolt,
                                    title = "No solar projects yet",
                                    description = "Add the first solar project for ${customer.fullName} to track capacity, quotation, and stage progression.",
                                    action = {
                                        Button(
                                            onClick = {
                                                viewModel.clearError()
                                                showAddProject = true
                                            }
                                        ) {
                                            Text("Create First Project")
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        items(
                            items = state.projects,
                            key = { it.id }
                        ) { project ->
                            Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                                ProjectCard(
                                    project = project,
                                    stages = state.availableStages,
                                    currencySymbol = state.currencySymbol,
                                    currencyCode = state.currencyCode,
                                    onMoveStage = {
                                        viewModel.clearError()
                                        stagingProject = project
                                    },
                                    onEdit = {
                                        viewModel.clearError()
                                        editingProject = project
                                    }
                                )
                            }
                        }
                    }

                    // Activity History Section
                    item {
                        Column(
                            modifier = Modifier
                                .widthIn(max = maxContentWidth)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionHeader(
                                title = "Activity History (${state.activities.size})",
                                subtitle = "Recorded changes and stage progressions"
                            )
                            if (state.activities.isEmpty()) {
                                SolarCard {
                                    Text(
                                        text = "No activity recorded yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                SolarCard {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        state.activities.forEachIndexed { idx, act ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
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
                                                        text = act.description,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "${act.id} • ${Formatters.formatDateTime(act.timestamp)}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (idx < state.activities.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditCustomer && state.customer != null) {
        CustomerFormDialog(
            initialCustomer = state.customer,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSave = { input ->
                viewModel.updateCustomer(input) {
                    showEditCustomer = false
                }
            },
            onDismiss = {
                viewModel.clearError()
                showEditCustomer = false
            }
        )
    }

    if (showArchiveConfirm && state.customer != null) {
        val current = state.customer!!
        val archiving = !current.isArchived
        ConfirmActionDialog(
            title = if (archiving) "Archive ${current.id}?" else "Restore ${current.id}?",
            message = if (archiving) {
                "${current.fullName} will be marked as archived."
            } else {
                "${current.fullName} will return to the active list."
            },
            confirmLabel = if (archiving) "Archive" else "Restore",
            isDestructive = archiving,
            onConfirm = {
                viewModel.toggleArchiveCustomer()
                showArchiveConfirm = false
            },
            onDismiss = { showArchiveConfirm = false }
        )
    }

    if (showAddProject && state.customer != null) {
        val customer = state.customer!!
        val defaultLoc = listOf(customer.address, customer.city)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        ProjectFormDialog(
            customerId = customer.id,
            defaultLocation = defaultLoc,
            availableStages = state.availableStages,
            currencySymbol = state.currencySymbol,
            initialProject = null,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSave = { input ->
                viewModel.createProject(input) {
                    showAddProject = false
                }
            },
            onDismiss = {
                viewModel.clearError()
                showAddProject = false
            }
        )
    }

    editingProject?.let { target ->
        ProjectFormDialog(
            customerId = target.customerId,
            defaultLocation = target.installationLocation,
            availableStages = state.availableStages,
            currencySymbol = state.currencySymbol,
            initialProject = target,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSave = { input ->
                viewModel.updateProject(target.id, input) {
                    editingProject = null
                }
            },
            onDismiss = {
                viewModel.clearError()
                editingProject = null
            }
        )
    }

    stagingProject?.let { target ->
        StageProgressionDialog(
            project = target,
            availableStages = state.availableStages,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSaveStage = { newStage, newStatus, note ->
                viewModel.updateProjectStage(
                    projectId = target.id,
                    newStage = newStage,
                    newStatus = newStatus,
                    note = note
                ) {
                    stagingProject = null
                }
            },
            onDismiss = {
                viewModel.clearError()
                stagingProject = null
            }
        )
    }
}

@Composable
private fun SummaryMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    stages: List<String>,
    currencySymbol: String,
    currencyCode: String,
    onMoveStage: () -> Unit,
    onEdit: () -> Unit
) {
    val status = runCatching { ProjectStatus.valueOf(project.status) }.getOrDefault(ProjectStatus.ACTIVE)
    val stageIndex = stages.indexOfFirst { it.equals(project.currentStage, ignoreCase = true) }
    val totalStages = stages.size.coerceAtLeast(1)
    val progressFraction = if (stageIndex >= 0) {
        (stageIndex + 1).toFloat() / totalStages.toFloat()
    } else {
        0f
    }

    SolarCard(modifier = Modifier.fillMaxWidth().testTag("project_card_${project.id}")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IdPill(project.id)
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                ProjectStatusBadge(status = status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Formatters.formatCapacityKw(project.capacityKw),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = project.projectType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stage Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stage: ${project.currentStage}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stageIndex >= 0) {
                        Text(
                            text = "Step ${stageIndex + 1} of $totalStages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Details grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Location: ${project.installationLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (project.assignedPerson.isNotBlank()) {
                    Text(
                        text = "Assigned: ${project.assignedPerson}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quotation: ${
                            Formatters.formatCurrency(
                                project.quotationAmount,
                                currencySymbol,
                                currencyCode
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    project.expectedCompletionDate?.let { due ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Target date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Target ${Formatters.formatDate(due)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (project.notes.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMoveStage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("move_stage_button_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Update Stage",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Move Stage")
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(0.75f)
                        .testTag("edit_project_button_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Project",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }
}
