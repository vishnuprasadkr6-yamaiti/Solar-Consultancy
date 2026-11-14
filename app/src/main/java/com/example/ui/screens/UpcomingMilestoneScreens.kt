package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.components.SolarCard
import com.example.ui.components.StatusPill

@Composable
fun PendingActionsPlaceholderScreen(
    onOpenCustomers: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    UpcomingModuleView(
        title = "Pending Actions",
        milestoneBadge = "Scheduled for Milestone 2",
        icon = Icons.AutoMirrored.Outlined.Assignment,
        summary = "Task & follow-up management is part of Milestone 2 alongside per-project Document Checklists.",
        capabilities = listOf(
            "Tasks linked to specific Customers (CUS-ID) and Projects (PRJ-ID)",
            "Priority levels (Low, Medium, High) and statuses (Open, In Progress, Completed)",
            "Smart follow-up suggestions from rejected or missing documents and overdue invoices",
            "Filters for Due Today, Upcoming, Overdue, and High Priority"
        ),
        onOpenCustomers = onOpenCustomers,
        onOpenDashboard = onOpenDashboard,
        modifier = modifier
    )
}

@Composable
fun InvoicesPaymentsPlaceholderScreen(
    onOpenCustomers: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    UpcomingModuleView(
        title = "Invoices & Payments",
        milestoneBadge = "Scheduled for Milestone 3",
        icon = Icons.Outlined.AccountBalanceWallet,
        summary = "Financial tracking, partial payment balances, and full ZIP backups are scheduled for Milestone 3.",
        capabilities = listOf(
            "Invoices in INR (₹) linked to Customers and Projects",
            "Partial payments with automatic remaining balance calculation",
            "Mandatory reason voiding to preserve full audit history",
            "CSV financial registers and full local ZIP backup & restore"
        ),
        onOpenCustomers = onOpenCustomers,
        onOpenDashboard = onOpenDashboard,
        modifier = modifier
    )
}

@Composable
private fun UpcomingModuleView(
    title: String,
    milestoneBadge: String,
    icon: ImageVector,
    summary: String,
    capabilities: List<String>,
    onOpenCustomers: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val maxContentWidth = if (maxWidth >= 840.dp) 760.dp else 640.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                SolarCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusPill(
                                    label = milestoneBadge,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Milestone 1 Foundation Active",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            capabilities.forEach { bullet ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(16.dp)
                                    )
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenCustomers,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Manage Customers")
                            }
                            OutlinedButton(
                                onClick = onOpenDashboard,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dashboard")
                            }
                        }
                    }
                }
            }
        }
    }
}
