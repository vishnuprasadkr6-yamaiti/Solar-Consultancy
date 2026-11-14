package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.repository.ProjectInput
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormDialog(
    customerId: String,
    defaultLocation: String,
    availableStages: List<String>,
    currencySymbol: String,
    initialProject: ProjectEntity? = null,
    isSaving: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onSave: (ProjectInput) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialProject) { mutableStateOf(initialProject?.name ?: "") }
    var capacityText by remember(initialProject) {
        mutableStateOf(
            if (initialProject != null) {
                initialProject.capacityKw.toString().removeSuffix(".0")
            } else {
                ""
            }
        )
    }
    var projectType by remember(initialProject) {
        mutableStateOf(initialProject?.projectType ?: ProjectType.RESIDENTIAL_ROOFTOP.label)
    }
    var location by remember(initialProject) {
        mutableStateOf(initialProject?.installationLocation ?: defaultLocation)
    }
    var assignedPerson by remember(initialProject) {
        mutableStateOf(initialProject?.assignedPerson ?: "")
    }
    var selectedStage by remember(initialProject, availableStages) {
        mutableStateOf(
            initialProject?.currentStage ?: availableStages.firstOrNull() ?: "Enquiry"
        )
    }
    var selectedStatus by remember(initialProject) {
        mutableStateOf(
            initialProject?.let { runCatching { ProjectStatus.valueOf(it.status) }.getOrNull() }
                ?: ProjectStatus.ACTIVE
        )
    }
    var expectedDateText by remember(initialProject) {
        mutableStateOf(Formatters.formatEditableDate(initialProject?.expectedCompletionDate))
    }
    var quotationText by remember(initialProject) {
        mutableStateOf(
            if (initialProject != null && initialProject.quotationAmount > 0) {
                initialProject.quotationAmount.toLong().toString()
            } else {
                ""
            }
        )
    }
    var notes by remember(initialProject) { mutableStateOf(initialProject?.notes ?: "") }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var stageExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialProject == null) "Add Solar Project" else "Edit ${initialProject.id}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Linked to customer $customerId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val displayError = localValidationError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ErrorBanner(
                        message = displayError,
                        onDismiss = {
                            localValidationError = null
                            onDismissError()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name / Reference *") },
                        placeholder = { Text("e.g., Main Residence 5kW On-Grid") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = capacityText,
                            onValueChange = { capacityText = it },
                            label = { Text("Capacity (kW) *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("project_capacity_input")
                        )
                        OutlinedTextField(
                            value = quotationText,
                            onValueChange = { quotationText = it },
                            label = { Text("Quotation ($currencySymbol)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("project_quotation_input")
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded }
                    ) {
                        OutlinedTextField(
                            value = projectType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("project_type_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            ProjectType.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        projectType = option.label
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Installation Location *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_location_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = stageExpanded,
                        onExpandedChange = { stageExpanded = !stageExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Stage") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("project_stage_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = stageExpanded,
                            onDismissRequest = { stageExpanded = false }
                        ) {
                            availableStages.forEach { stage ->
                                DropdownMenuItem(
                                    text = { Text(stage) },
                                    onClick = {
                                        selectedStage = stage
                                        if (stage.equals("Completed", ignoreCase = true)) {
                                            selectedStatus = ProjectStatus.COMPLETED
                                        }
                                        stageExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedStatus.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("project_status_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                ProjectStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.label) },
                                        onClick = {
                                            selectedStatus = status
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = expectedDateText,
                            onValueChange = { expectedDateText = it },
                            label = { Text("Target Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("project_target_date_input")
                        )
                    }

                    OutlinedTextField(
                        value = assignedPerson,
                        onValueChange = { assignedPerson = it },
                        label = { Text("Assigned Engineer / Person (Optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_assigned_input")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Project Notes") },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_notes_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            localValidationError = null
                            val parsedKw = capacityText.trim().toDoubleOrNull()
                            if (parsedKw == null || parsedKw <= 0.0) {
                                localValidationError = "Enter a valid solar capacity in kW (greater than 0)."
                                return@Button
                            }
                            val parsedQuotation = if (quotationText.isBlank()) {
                                0.0
                            } else {
                                quotationText.trim().replace(",", "").toDoubleOrNull()
                            }
                            if (parsedQuotation == null || parsedQuotation < 0.0) {
                                localValidationError = "Enter a valid non-negative quotation amount."
                                return@Button
                            }
                            val parsedDate = if (expectedDateText.isBlank()) {
                                null
                            } else {
                                Formatters.parseEditableDate(expectedDateText).also { parsed ->
                                    if (parsed == null) {
                                        localValidationError = "Target date must use YYYY-MM-DD format (e.g., 2026-12-30)."
                                    }
                                }
                            }
                            if (localValidationError != null) return@Button

                            onSave(
                                ProjectInput(
                                    customerId = customerId,
                                    name = name,
                                    capacityKw = parsedKw,
                                    projectType = projectType,
                                    installationLocation = location,
                                    assignedPerson = assignedPerson,
                                    currentStage = selectedStage,
                                    status = selectedStatus,
                                    expectedCompletionDate = parsedDate,
                                    quotationAmount = parsedQuotation,
                                    notes = notes
                                )
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_save_button")
                    ) {
                        Text(if (isSaving) "Saving..." else "Save Project")
                    }
                }
            }
        }
    }
}
