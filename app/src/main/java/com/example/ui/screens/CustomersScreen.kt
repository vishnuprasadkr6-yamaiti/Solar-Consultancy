package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerEntity
import com.example.data.model.CustomerFilterOption
import com.example.data.model.CustomerSortOption
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IdPill
import com.example.ui.components.SolarCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusArchivedBg
import com.example.ui.theme.StatusArchivedText
import com.example.util.Formatters

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onOpenCustomer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var archiveTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearError()
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxContentWidth = if (maxWidth >= 840.dp) 1080.dp else 720.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("customers_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customers",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${state.totalActiveCount} active • ${state.totalArchivedCount} archived",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    showAddDialog = true
                                },
                                modifier = Modifier.testTag("add_customer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Add Customer",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add")
                            }
                        }

                        // Search bar
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search by name, phone, location, or CUS-ID") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = "Search customers")
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_search_input")
                        )

                        // Filter + Sort bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomerFilterOption.entries.forEach { option ->
                                    FilterChip(
                                        selected = state.filterOption == option,
                                        onClick = { viewModel.onFilterChange(option) },
                                        label = { Text(option.label) },
                                        modifier = Modifier.testTag("customer_filter_${option.name.lowercase()}")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { sortMenuExpanded = true },
                                    modifier = Modifier.testTag("customer_sort_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = "Sort customers",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sort", maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    CustomerSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                viewModel.onSortChange(option)
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!state.isLoading && state.customers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                            EmptyStateCard(
                                icon = Icons.Outlined.PeopleOutline,
                                title = if (state.searchQuery.isBlank()) {
                                    "No customers in this view"
                                } else {
                                    "No matching customers"
                                },
                                description = if (state.searchQuery.isBlank()) {
                                    "Create a customer record and add a project to track checklists, site photos, and offline PDF quotes."
                                } else {
                                    "Try clearing your search or switching between Active, Archived, and All filters."
                                },
                                action = {
                                    if (state.searchQuery.isBlank()) {
                                        Button(
                                            onClick = {
                                                viewModel.clearError()
                                                showAddDialog = true
                                            }
                                        ) {
                                            Text("Add Customer")
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    items(
                        items = state.customers,
                        key = { it.customer.id }
                    ) { item ->
                        Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                            CustomerListCard(
                                item = item,
                                onOpen = { onOpenCustomer(item.customer.id) },
                                onEdit = {
                                    viewModel.clearError()
                                    editingCustomer = item.customer
                                },
                                onToggleArchive = { archiveTarget = item.customer }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerFormDialog(
            initialCustomer = null,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSave = { input ->
                viewModel.createCustomer(input) { newId ->
                    showAddDialog = false
                    onOpenCustomer(newId)
                }
            },
            onDismiss = {
                viewModel.clearError()
                showAddDialog = false
            }
        )
    }

    editingCustomer?.let { target ->
        CustomerFormDialog(
            initialCustomer = target,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismissError = viewModel::clearError,
            onSave = { input ->
                viewModel.updateCustomer(target.id, input) {
                    editingCustomer = null
                }
            },
            onDismiss = {
                viewModel.clearError()
                editingCustomer = null
            }
        )
    }

    archiveTarget?.let { target ->
        val archiving = !target.isArchived
        ConfirmActionDialog(
            title = if (archiving) "Archive ${target.id}?" else "Restore ${target.id}?",
            message = if (archiving) {
                "${target.fullName} will move to Archived. Existing projects and history remain intact."
            } else {
                "${target.fullName} will be restored to your active customer list."
            },
            confirmLabel = if (archiving) "Archive" else "Restore",
            isDestructive = archiving,
            onConfirm = {
                viewModel.setArchived(target.id, archiving)
                archiveTarget = null
            },
            onDismiss = { archiveTarget = null }
        )
    }
}

@Composable
private fun CustomerListCard(
    item: CustomerListItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val customer = item.customer
    var menuExpanded by remember { mutableStateOf(false) }

    SolarCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    IdPill(customer.id)
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = item.primaryStatusLabel,
                        containerColor = if (customer.isArchived) StatusArchivedBg else StatusActiveBg,
                        contentColor = if (customer.isArchived) StatusArchivedText else StatusActiveText
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("customer_menu_${customer.id}")
                        ) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Customer options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Customer") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (customer.isArchived) "Restore Customer" else "Archive Customer") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (customer.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleArchive()
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = customer.primaryPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (customer.city.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = customer.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = "Capacity",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${item.totalProjects} projects • ${Formatters.formatCapacityKw(item.totalCapacityKw)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = Formatters.formatDate(customer.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
