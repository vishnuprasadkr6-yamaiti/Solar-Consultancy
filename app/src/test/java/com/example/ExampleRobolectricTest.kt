package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesStore
import com.example.data.model.DefaultStages
import com.example.data.model.ProjectStatus
import com.example.data.repository.CustomerInput
import com.example.data.repository.ProjectInput
import com.example.data.repository.SolarRepository
import com.example.util.Formatters
import com.example.util.QuoteInputData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: SolarRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SolarRepository(
            context = context,
            db = db,
            preferencesStore = PreferencesStore(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches SolarDesk`() {
        assertEquals("SolarDesk", context.getString(R.string.app_name))
    }

    @Test
    fun `default stages match redefined 12 stage solar pipeline`() = runTest {
        val expectedStages = listOf(
            "Work Order Signing",
            "Document Collection",
            "MNRE Registration",
            "Feasibility Assessment",
            "System Delivery",
            "Industrial Works",
            "Electrical Works",
            "Completion Report Submission",
            "Net Metering",
            "Application Installation",
            "Second Loan Disbursement",
            "Subsidy Processing"
        )
        assertEquals(expectedStages, DefaultStages.STAGES)

        repository.ensureDefaultSettings()
        val settings = repository.getSettingsSnapshot()
        assertEquals(expectedStages, DefaultStages.decodeStages(settings.stagesEncoded))
    }

    @Test
    fun `create edit archive customer and multiple projects with stage transitions`() = runTest {
        repository.ensureDefaultSettings()

        val customer = repository.createCustomer(
            CustomerInput(
                fullName = "Aarav Menon",
                primaryPhone = "9876543210",
                email = "aarav@example.com",
                city = "Kochi"
            )
        ).getOrThrow()
        assertEquals("CUS-00001", customer.id)

        val updatedCustomer = repository.updateCustomer(
            customer.id,
            CustomerInput(
                fullName = "Aarav K. Menon",
                primaryPhone = "9876543210",
                city = "Ernakulam"
            )
        ).getOrThrow()
        assertEquals("Ernakulam", updatedCustomer.city)

        val prj1 = repository.createProject(
            ProjectInput(
                customerId = customer.id,
                name = "Residence 5kW Rooftop",
                capacityKw = 5.0,
                projectType = "Residential Rooftop",
                installationLocation = "Kakkanad, Kochi",
                currentStage = "Work Order Signing",
                quotationAmount = 325000.0
            )
        ).getOrThrow()
        assertEquals("PRJ-00001", prj1.id)

        val prj2 = repository.createProject(
            ProjectInput(
                customerId = customer.id,
                name = "Warehouse 25kW Commercial",
                capacityKw = 25.0,
                projectType = "Commercial Rooftop",
                installationLocation = "Aluva, Kochi",
                currentStage = "Feasibility Assessment",
                quotationAmount = 1450000.0
            )
        ).getOrThrow()
        assertEquals("PRJ-00002", prj2.id)

        val stageUpdate = repository.updateProjectStage(
            projectId = prj1.id,
            newStage = "MNRE Registration",
            newStatus = ProjectStatus.ACTIVE,
            stageNote = "MNRE portal application submitted"
        )
        assertTrue(stageUpdate.isSuccess)

        val projects = repository.observeCustomerProjects(customer.id).first()
        assertEquals(2, projects.size)
        assertEquals("MNRE Registration", projects.first { it.id == prj1.id }.currentStage)

        val activities = repository.observeCustomerActivity(customer.id).first()
        assertTrue(activities.any { it.description.contains("moved from 'Work Order Signing' to 'MNRE Registration'") })

        val archiveRes = repository.setCustomerArchived(customer.id, true)
        assertTrue(archiveRes.isSuccess)
        assertTrue(repository.observeCustomer(customer.id).first()!!.isArchived)
    }

    @Test
    fun `project checklists seed 12 steps toggle completion and report dashboard KPI summary`() = runTest {
        repository.ensureDefaultSettings()
        val customer = repository.createCustomer(
            CustomerInput(fullName = "Meera Nair", primaryPhone = "9847012345")
        ).getOrThrow()

        val prj = repository.createProject(
            ProjectInput(
                customerId = customer.id,
                name = "Home 3kW On-Grid",
                capacityKw = 3.0,
                projectType = "Residential Rooftop",
                installationLocation = "Thrissur",
                currentStage = "Work Order Signing",
                quotationAmount = 210000.0
            )
        ).getOrThrow()

        val checklist = repository.observeCustomerChecklists(customer.id).first()
        assertEquals(12, checklist.size)

        var summary = repository.checklistCompletionSummary.first()
        assertEquals(1, summary.size)
        assertEquals(12, summary.first().totalItems)
        assertEquals(0, summary.first().completedItems)

        checklist.forEach { item ->
            assertTrue(repository.toggleChecklistItem(item.id, true).isSuccess)
        }

        summary = repository.checklistCompletionSummary.first()
        assertEquals(12, summary.first().completedItems)

        val customAdded = repository.addCustomChecklistItem(prj.id, "Warranty Card Handover")
        assertTrue(customAdded.isSuccess)
        summary = repository.checklistCompletionSummary.first()
        assertEquals(13, summary.first().totalItems)
        assertEquals(12, summary.first().completedItems)
    }

    @Test
    fun `local photo attachment persists URI string and PDF quote generates offline`() = runTest {
        repository.ensureDefaultSettings()
        val customer = repository.createCustomer(
            CustomerInput(fullName = "Rohan Varma", primaryPhone = "9447112233", city = "Kozhikode")
        ).getOrThrow()
        val project = repository.createProject(
            ProjectInput(
                customerId = customer.id,
                name = "Villa 8kW Hybrid",
                capacityKw = 8.0,
                projectType = "Hybrid + Battery",
                installationLocation = "Beach Road, Kozhikode",
                currentStage = "Work Order Signing",
                quotationAmount = 480000.0
            )
        ).getOrThrow()

        val (photoFile, photoUri) = repository.createPhotoOutputUri(project.id)
        photoFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        assertTrue(photoUri.toString().startsWith("content://${context.packageName}.fileprovider/project_photos/${project.id}/"))

        val savedPhoto = repository.saveProjectPhoto(project.id, photoFile, photoUri).getOrThrow()
        assertEquals(photoUri.toString(), savedPhoto.localUri)

        val attachedPhotos = repository.observeProjectPhotos(project.id).first()
        assertEquals(1, attachedPhotos.size)

        val quoteInput = QuoteInputData(
            systemPrice = 450000.0,
            ksebFeasibilityFee = 1500.0,
            ksebPlantRegistrationFee = 2000.0,
            ksebAgreementFee = 500.0,
            ksebLtMeterFee = 3500.0,
            structureCost = 18000.0,
            additionalCableCost = 4500.0,
            deliveryPeriod = "7 Days",
            installationPeriod = "4 Days",
            netMeterPeriod = "21 Days",
            notes = "Includes 5 years workmanship support"
        )
        assertEquals(7500.0, quoteInput.totalKsebFees, 0.01)
        assertEquals(480000.0, quoteInput.grandTotal, 0.01)

        assertTrue(repository.recordQuoteGenerated(project.id, quoteInput.grandTotal, true).isSuccess)
        val updatedProject = repository.getProjectById(project.id)!!
        assertEquals(480000.0, updatedProject.quotationAmount, 0.01)

        val activities = repository.observeCustomerActivity(customer.id).first()
        assertTrue(activities.any { it.entityType == "QUOTE" && it.description.contains("480000") })

        assertTrue(repository.deleteProjectPhoto(savedPhoto.id).isSuccess)
        assertFalse(photoFile.exists())
        assertTrue(repository.observeProjectPhotos(project.id).first().isEmpty())
    }

    @Test
    fun `indian rupee formatting matches lakhs notation`() {
        assertEquals("₹1,00,000", Formatters.formatCurrency(100000.0, "₹", "INR"))
        assertEquals("₹60,000", Formatters.formatCurrency(60000.0, "₹", "INR"))
        assertEquals("5 kW", Formatters.formatCapacityKw(5.0))
        assertEquals("3.5 kW", Formatters.formatCapacityKw(3.5))
    }
}
