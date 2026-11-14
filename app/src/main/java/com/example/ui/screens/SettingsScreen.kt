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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultStages
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LocalOnlyNoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.SolarCard
import com.example.util.Formatters

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var businessName by remember(state.settings) { mutableStateOf(state.settings.businessName) }
    var businessPhone by remember(state.settings) { mutableStateOf(state.settings.businessPhone) }
    var businessEmail by remember(state.settings) { mutableStateOf(state.settings.businessEmail) }
    var businessAddress by remember(state.settings) { mutableStateOf(state.settings.businessAddress) }
    var taxId by remember(state.settings) { mutableStateOf(state.settings.taxOrGstId) }

    val editableStages = remember(state.stages) {
        mutableStateListOf<String>().apply { addAll(state.stages) }
    }
    var newStageName by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxContentWidth = if (maxWidth >= 840.dp) 900.dp else 720.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LocalOnlyNoticeBanner()

                        if (!state.errorMessage.isNullOrBlank()) {
                            ErrorBanner(
                                message = state.errorMessage!!,
                                onDismiss = viewModel::clearError
                            )
                        }
                    }
                }

                // Business Info Card
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Business Information",
                            subtitle = "Displayed in your workspace header and future invoices"
                        )
                        SolarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text("Consultancy / Business Name *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_business_name_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = businessPhone,
                                        onValueChange = { businessPhone = it },
                                        label = { Text("Phone") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = taxId,
                                        onValueChange = { taxId = it },
                                        label = { Text("GST / Tax ID") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = businessEmail,
                                    onValueChange = { businessEmail = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = businessAddress,
                                    onValueChange = { businessAddress = it },
                                    label = { Text("Office Address") },
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Project Stage Configuration
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Project Stages (${editableStages.size})",
                            subtitle = "Configure default solar project lifecycle stages",
                            action = {
                                TextButton(
                                    onClick = {
                                        editableStages.clear()
                                        editableStages.addAll(DefaultStages.STAGES)
                                    },
                                    modifier = Modifier.testTag("reset_stages_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RestartAlt,
                                        contentDescription = "Reset stages",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Defaults")
                                }
                            }
                        )
                        SolarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                editableStages.forEachIndexed { index, stage ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        OutlinedTextField(
                                            value = stage,
                                            onValueChange = { editableStages[index] = it },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val tmp = editableStages[index - 1]
                                                    editableStages[index - 1] = editableStages[index]
                                                    editableStages[index] = tmp
                                                }
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move stage up")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < editableStages.lastIndex) {
                                                    val tmp = editableStages[index + 1]
                                                    editableStages[index + 1] = editableStages[index]
                                                    editableStages[index] = tmp
                                                }
                                            },
                                            enabled = index < editableStages.lastIndex
                                        ) {
                                            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move stage down")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (editableStages.size > 2) {
                                                    editableStages.removeAt(index)
                                                }
                                            },
                                            enabled = editableStages.size > 2
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove stage")
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newStageName,
                                        onValueChange = { newStageName = it },
                                        label = { Text("Add new stage") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val clean = newStageName.trim()
                                            if (clean.isNotEmpty() && !editableStages.any { it.equals(clean, ignoreCase = true) }) {
                                                editableStages.add(clean)
                                                newStageName = ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Add stage", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveSettings(
                                    businessName = businessName,
                                    businessPhone = businessPhone,
                                    businessEmail = businessEmail,
                                    businessAddress = businessAddress,
                                    taxOrGstId = taxId,
                                    currencySymbol = "₹",
                                    currencyCode = "INR",
                                    stages = editableStages.toList()
                                )
                            },
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_settings_button")
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = "Save settings", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isSaving) "Saving..." else "Save Business & Stage Settings")
                        }
                    }
                }

                // Local Database & Storage Info
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Database & Device Storage",
                            subtitle = "SQLite Room persistence on this device only",
                            action = {
                                IconButton(onClick = viewModel::refreshDatabaseStats) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh storage stats")
                                }
                            }
                        )
                        SolarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Storage,
                                        contentDescription = "Local database",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Local Database Size: ${Formatters.formatBytes(state.dbStats.databaseSizeBytes)}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${state.dbStats.customerCount} customers • ${state.dbStats.projectCount} projects • ${state.dbStats.activityCount} activity entries",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Reset Application Data
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Danger Zone",
                            subtitle = "Irreversible local database actions"
                        )
                        SolarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Reset All Local Data",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = "Permanently erases all customers, solar projects, activity history, and custom settings from this device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { showResetDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("reset_all_data_button")
                                ) {
                                    Text("Reset Application Data")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Full Local Reset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This permanently deletes all customers, projects, and activity history stored on this device. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        label = { Text("Type RESET to confirm") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_confirm_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData {
                            showResetDialog = false
                        }
                    },
                    enabled = confirmText.trim().equals("RESET", ignoreCase = false) && !state.isSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("reset_confirm_button")
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
