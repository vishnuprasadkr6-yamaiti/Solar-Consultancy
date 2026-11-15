package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IdPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.SolarCard
import com.example.util.Formatters
import com.example.util.QuoteInputData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    viewModel: CreateQuoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var systemPriceText by remember(state.project) {
        mutableStateOf(
            state.project?.quotationAmount?.takeIf { it > 0 }?.toLong()?.toString() ?: ""
        )
    }
    var ksebFeasibilityText by remember { mutableStateOf("0") }
    var ksebPlantRegText by remember { mutableStateOf("0") }
    var ksebAgreementText by remember { mutableStateOf("0") }
    var ksebLtMeterText by remember { mutableStateOf("0") }
    var structureCostText by remember { mutableStateOf("0") }
    var additionalCableText by remember { mutableStateOf("0") }

    var deliveryPeriod by remember { mutableStateOf("7–10 Days") }
    var installationPeriod by remember { mutableStateOf("3–5 Days") }
    var netMeterPeriod by remember { mutableStateOf("15–30 Days") }
    var quoteNotes by remember { mutableStateOf("") }
    var updateProjectQuotation by remember { mutableStateOf(true) }

    val parsedQuote = remember(
        systemPriceText,
        ksebFeasibilityText,
        ksebPlantRegText,
        ksebAgreementText,
        ksebLtMeterText,
        structureCostText,
        additionalCableText,
        deliveryPeriod,
        installationPeriod,
        netMeterPeriod,
        quoteNotes
    ) {
        QuoteInputData(
            systemPrice = systemPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            ksebFeasibilityFee = ksebFeasibilityText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            ksebPlantRegistrationFee = ksebPlantRegText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            ksebAgreementFee = ksebAgreementText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            ksebLtMeterFee = ksebLtMeterText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            structureCost = structureCostText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            additionalCableCost = additionalCableText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            deliveryPeriod = deliveryPeriod.trim(),
            installationPeriod = installationPeriod.trim(),
            netMeterPeriod = netMeterPeriod.trim(),
            notes = quoteNotes.trim()
        )
    }

    LaunchedEffect(state.generatedResult) {
        val generated = state.generatedResult ?: return@LaunchedEffect
        val project = state.project
        val customer = state.customer
        val summaryText = buildString {
            append("Solar Quotation for ${customer?.fullName ?: ""}")
            if (project != null) {
                append(" (${project.name} - ${Formatters.formatCapacityKw(project.capacityKw)})")
            }
            append("\nTotal: ${Formatters.formatCurrency(generated.grandTotal, "₹", "INR")}")
        }
        sharePdfViaWhatsApp(context, generated.contentUri, summaryText)
        snackbarHostState.showSnackbar("Quote PDF saved & share dialog opened.")
        viewModel.clearGeneratedResult()
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
                            text = "Create Solar Quote",
                            style = MaterialTheme.typography.titleMedium
                        )
                        state.project?.let {
                            Text(
                                text = "${it.id} • ${it.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_quote_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val project = state.project
        val customer = state.customer

        if (!state.isLoading && (project == null || customer == null)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = "Project not found",
                    description = "Unable to load project details for quotation.",
                    action = { Button(onClick = onBack) { Text("Go Back") } }
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val maxContentWidth = if (maxWidth >= 840.dp) 820.dp else 680.dp

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
                    if (project != null && customer != null) {
                        SolarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IdPill(project.id)
                                    Text(
                                        text = customer.fullName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(
                                    text = "${project.name} • ${Formatters.formatCapacityKw(project.capacityKw)} • ${project.installationLocation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        ErrorBanner(
                            message = state.errorMessage!!,
                            onDismiss = viewModel::clearError
                        )
                    }

                    // System & Hardware Costs
                    SectionHeader(
                        title = "Pricing Breakdown (INR ₹)",
                        subtitle = "Enter system, KSEB, structure, and cable amounts"
                    )

                    SolarCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumericRupeeField(
                                value = systemPriceText,
                                onValueChange = { systemPriceText = it },
                                label = "System Price (₹) *",
                                tag = "quote_system_price_input"
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "KSEB Fees (₹)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NumericRupeeField(
                                    value = ksebFeasibilityText,
                                    onValueChange = { ksebFeasibilityText = it },
                                    label = "Feasibility Fee",
                                    tag = "quote_kseb_feasibility_input",
                                    modifier = Modifier.weight(1f)
                                )
                                NumericRupeeField(
                                    value = ksebPlantRegText,
                                    onValueChange = { ksebPlantRegText = it },
                                    label = "Plant Registration",
                                    tag = "quote_kseb_plant_reg_input",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NumericRupeeField(
                                    value = ksebAgreementText,
                                    onValueChange = { ksebAgreementText = it },
                                    label = "Agreement Fee",
                                    tag = "quote_kseb_agreement_input",
                                    modifier = Modifier.weight(1f)
                                )
                                NumericRupeeField(
                                    value = ksebLtMeterText,
                                    onValueChange = { ksebLtMeterText = it },
                                    label = "LT Meter Fee",
                                    tag = "quote_kseb_lt_meter_input",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Additional Works (₹)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NumericRupeeField(
                                    value = structureCostText,
                                    onValueChange = { structureCostText = it },
                                    label = "Structure Cost",
                                    tag = "quote_structure_cost_input",
                                    modifier = Modifier.weight(1f)
                                )
                                NumericRupeeField(
                                    value = additionalCableText,
                                    onValueChange = { additionalCableText = it },
                                    label = "Additional Cable Cost",
                                    tag = "quote_cable_cost_input",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Execution Periods
                    SectionHeader(
                        title = "Schedule & Periods",
                        subtitle = "Estimated timelines displayed on the PDF quote"
                    )

                    SolarCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = deliveryPeriod,
                                onValueChange = { deliveryPeriod = it },
                                label = { Text("Delivery Period *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quote_delivery_period_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = installationPeriod,
                                    onValueChange = { installationPeriod = it },
                                    label = { Text("Installation Period *") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quote_installation_period_input")
                                )
                                OutlinedTextField(
                                    value = netMeterPeriod,
                                    onValueChange = { netMeterPeriod = it },
                                    label = { Text("Net Meter Period *") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quote_net_meter_period_input")
                                )
                            }
                            OutlinedTextField(
                                value = quoteNotes,
                                onValueChange = { quoteNotes = it },
                                label = { Text("Additional Terms / Notes (Optional)") },
                                minLines = 2,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Live Grand Total Card
                    SolarCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("KSEB Fees Subtotal", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = Formatters.formatCurrency(parsedQuote.totalKsebFees, "₹", "INR"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grand Total",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = Formatters.formatCurrency(parsedQuote.grandTotal, "₹", "INR"),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = updateProjectQuotation,
                                    onCheckedChange = { updateProjectQuotation = it }
                                )
                                Text(
                                    text = "Update project quotation amount with this total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !state.isGenerating,
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.generateQuotePdf(
                                    context = context,
                                    input = parsedQuote,
                                    updateProjectQuotation = updateProjectQuotation
                                )
                            },
                            enabled = !state.isGenerating,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("generate_share_quote_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Share Quote PDF",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isGenerating) "Generating PDF..." else "Generate PDF & Share on WhatsApp")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NumericRupeeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (next.isEmpty() || next.all { it.isDigit() || it == '.' }) {
                onValueChange(next)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

private fun sharePdfViaWhatsApp(
    context: Context,
    pdfUri: Uri,
    messageText: String
) {
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_TEXT, messageText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val whatsappIntent = Intent(baseIntent).apply { setPackage("com.whatsapp") }
    try {
        context.startActivity(whatsappIntent)
    } catch (_: ActivityNotFoundException) {
        val whatsappBusinessIntent = Intent(baseIntent).apply { setPackage("com.whatsapp.w4b") }
        try {
            context.startActivity(whatsappBusinessIntent)
        } catch (_: ActivityNotFoundException) {
            val chooser = Intent.createChooser(baseIntent, "Share Solar Quote PDF")
            context.startActivity(chooser)
        }
    }
}
