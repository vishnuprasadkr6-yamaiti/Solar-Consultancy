package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsEntity
import java.io.File
import java.io.FileOutputStream

data class QuoteInputData(
    val systemPrice: Double,
    val ksebFeasibilityFee: Double,
    val ksebPlantRegistrationFee: Double,
    val ksebAgreementFee: Double,
    val ksebLtMeterFee: Double,
    val structureCost: Double,
    val additionalCableCost: Double,
    val deliveryPeriod: String,
    val installationPeriod: String,
    val netMeterPeriod: String,
    val notes: String = ""
) {
    val totalKsebFees: Double
        get() = ksebFeasibilityFee + ksebPlantRegistrationFee + ksebAgreementFee + ksebLtMeterFee

    val grandTotal: Double
        get() = systemPrice + totalKsebFees + structureCost + additionalCableCost
}

data class GeneratedQuoteResult(
    val file: File,
    val contentUri: Uri,
    val grandTotal: Double
)

object QuotePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f

    fun generateQuotePdf(
        context: Context,
        settings: SettingsEntity,
        customer: CustomerEntity,
        project: ProjectEntity,
        quote: QuoteInputData
    ): GeneratedQuoteResult {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val greenColor = Color.rgb(27, 94, 63)
        val darkText = Color.rgb(25, 29, 26)
        val mutedText = Color.rgb(85, 94, 88)
        val borderColor = Color.rgb(216, 223, 217)
        val lightBg = Color.rgb(244, 247, 244)

        val headerBgPaint = Paint().apply {
            color = greenColor
            style = Paint.Style.FILL
        }
        val boxBgPaint = Paint().apply {
            color = lightBg
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
        }
        val titleWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(227, 241, 233)
            textSize = 10f
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 10.5f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 9.5f
        }
        val rightAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
        }
        val totalAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // 1. Header Banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 96f, headerBgPaint)
        canvas.drawText(settings.businessName.ifBlank { "Solar Consultancy" }, MARGIN, 38f, titleWhitePaint)
        val contactLine = listOf(settings.businessPhone, settings.businessEmail, settings.taxOrGstId)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, MARGIN, 56f, subWhitePaint)
        }
        if (settings.businessAddress.isNotBlank()) {
            canvas.drawText(settings.businessAddress.take(80), MARGIN, 72f, subWhitePaint)
        }

        val quoteBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val quoteSubRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(227, 241, 233)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val rightX = PAGE_WIDTH - MARGIN
        canvas.drawText("SOLAR QUOTATION", rightX, 40f, quoteBadgePaint)
        canvas.drawText("Ref: QT-${project.id}", rightX, 58f, quoteSubRightPaint)
        canvas.drawText("Date: ${Formatters.formatDate(System.currentTimeMillis())}", rightX, 74f, quoteSubRightPaint)

        var y = 122f

        // 2. Customer & Project Summary Box
        canvas.drawRect(MARGIN, y, rightX, y + 86f, boxBgPaint)
        canvas.drawText("PREPARED FOR", MARGIN + 12f, y + 18f, sectionTitlePaint)
        canvas.drawText("${customer.fullName} (${customer.id})", MARGIN + 12f, y + 36f, bodyBoldPaint)
        canvas.drawText("Phone: ${customer.primaryPhone}", MARGIN + 12f, y + 52f, bodyPaint)
        val custAddr = listOf(customer.address, customer.city, customer.postalCode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        canvas.drawText(
            if (custAddr.isNotBlank()) custAddr.take(44) else "Location: ${project.installationLocation}",
            MARGIN + 12f,
            y + 68f,
            mutedPaint
        )

        val midX = PAGE_WIDTH / 2f + 10f
        canvas.drawText("PROJECT DETAILS", midX, y + 18f, sectionTitlePaint)
        canvas.drawText("${project.name} (${project.id})", midX, y + 36f, bodyBoldPaint)
        canvas.drawText(
            "Capacity: ${Formatters.formatCapacityKw(project.capacityKw)}  •  ${project.projectType}",
            midX,
            y + 52f,
            bodyPaint
        )
        canvas.drawText("Site: ${project.installationLocation.take(38)}", midX, y + 68f, mutedPaint)

        y += 110f

        // 3. Cost Breakdown Table
        canvas.drawText("COST BREAKDOWN (INR)", MARGIN, y, sectionTitlePaint)
        y += 10f
        canvas.drawRect(MARGIN, y, rightX, y + 26f, boxBgPaint)
        canvas.drawText("Description", MARGIN + 10f, y + 17f, bodyBoldPaint)
        val rightHeaderPaint = Paint(bodyBoldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Amount", rightX - 10f, y + 17f, rightHeaderPaint)
        y += 26f

        val costRows = listOf(
            "Solar System Price (${Formatters.formatCapacityKw(project.capacityKw)})" to quote.systemPrice,
            "KSEB Feasibility Fee" to quote.ksebFeasibilityFee,
            "KSEB Plant Registration Fee" to quote.ksebPlantRegistrationFee,
            "KSEB Agreement Fee" to quote.ksebAgreementFee,
            "KSEB LT Meter Fee" to quote.ksebLtMeterFee,
            "Mounting Structure Cost" to quote.structureCost,
            "Additional Cable Cost" to quote.additionalCableCost
        )

        costRows.forEach { (label, amount) ->
            canvas.drawLine(MARGIN, y, rightX, y, linePaint)
            canvas.drawText(label, MARGIN + 10f, y + 18f, bodyPaint)
            canvas.drawText(Formatters.formatCurrency(amount, "₹", "INR"), rightX - 10f, y + 18f, rightAmountPaint)
            y += 26f
        }

        // Grand Total Row
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        canvas.drawRect(MARGIN, y, rightX, y + 34f, boxBgPaint)
        val totalLabelPaint = Paint(sectionTitlePaint).apply { textSize = 12.5f }
        canvas.drawText("TOTAL QUOTATION AMOUNT", MARGIN + 10f, y + 22f, totalLabelPaint)
        canvas.drawText(
            Formatters.formatCurrency(quote.grandTotal, "₹", "INR"),
            rightX - 10f,
            y + 22f,
            totalAmountPaint
        )
        y += 34f
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)

        y += 28f

        // 4. Execution Timeline Table
        canvas.drawText("ESTIMATED TIMELINE", MARGIN, y, sectionTitlePaint)
        y += 10f
        val timelineRows = listOf(
            "Delivery Period" to quote.deliveryPeriod,
            "Installation Period" to quote.installationPeriod,
            "Net Meter Period" to quote.netMeterPeriod
        )
        timelineRows.forEach { (label, value) ->
            canvas.drawLine(MARGIN, y, rightX, y, linePaint)
            canvas.drawText(label, MARGIN + 10f, y + 18f, bodyBoldPaint)
            canvas.drawText(value.ifBlank { "As scheduled" }, rightX - 10f, y + 18f, rightAmountPaint)
            y += 26f
        }
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)

        if (quote.notes.isNotBlank()) {
            y += 26f
            canvas.drawText("TERMS & NOTES", MARGIN, y, sectionTitlePaint)
            y += 16f
            quote.notes.lines().take(5).forEach { noteLine ->
                canvas.drawText(noteLine.take(90), MARGIN, y, bodyPaint)
                y += 15f
            }
        }

        // Footer
        val footerY = PAGE_HEIGHT - 44f
        canvas.drawLine(MARGIN, footerY - 12f, rightX, footerY - 12f, linePaint)
        canvas.drawText(
            "Generated offline by ${settings.businessName.ifBlank { "SolarDesk" }} • Valid subject to final site inspection.",
            MARGIN,
            footerY + 4f,
            mutedPaint
        )

        document.finishPage(page)

        val quotesDir = File(context.filesDir, "quotes").apply { mkdirs() }
        val file = File(quotesDir, "Quote_${project.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return GeneratedQuoteResult(
            file = file,
            contentUri = uri,
            grandTotal = quote.grandTotal
        )
    }
}
