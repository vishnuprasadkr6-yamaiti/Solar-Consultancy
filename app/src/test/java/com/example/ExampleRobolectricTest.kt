package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesStore
import com.example.data.model.ProjectStatus
import com.example.data.repository.CustomerInput
import com.example.data.repository.ProjectInput
import com.example.data.repository.SolarRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `create edit archive customer and multiple projects with stage transitions`() = runTest {
        repository.ensureDefaultSettings()

        val customerResult = repository.createCustomer(
            CustomerInput(
                fullName = "Aarav Menon",
                primaryPhone = "9876543210",
                email = "aarav@example.com",
                city = "Kochi"
            )
        )
        assertTrue(customerResult.isSuccess)
        val customer = customerResult.getOrThrow()
        assertEquals("CUS-00001", customer.id)

        val updateResult = repository.updateCustomer(
            customer.id,
            CustomerInput(
                fullName = "Aarav K. Menon",
                primaryPhone = "9876543210",
                city = "Ernakulam"
            )
        )
        assertTrue(updateResult.isSuccess)
        assertEquals("Ernakulam", updateResult.getOrThrow().city)

        val prj1 = repository.createProject(
            ProjectInput(
                customerId = customer.id,
                name = "Residence 5kW Rooftop",
                capacityKw = 5.0,
                projectType = "Residential Rooftop",
                installationLocation = "Kakkanad, Kochi",
                currentStage = "Enquiry",
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
                currentStage = "Technical Survey",
                quotationAmount = 1450000.0
            )
        ).getOrThrow()
        assertEquals("PRJ-00002", prj2.id)

        val stageUpdate = repository.updateProjectStage(
            projectId = prj1.id,
            newStage = "Quotation",
            newStatus = ProjectStatus.ACTIVE,
            stageNote = "Skipped site survey using approved drawings"
        )
        assertTrue(stageUpdate.isSuccess)

        val projects = repository.observeCustomerProjects(customer.id).first()
        assertEquals(2, projects.size)
        assertEquals("Quotation", projects.first { it.id == prj1.id }.currentStage)

        val activities = repository.observeCustomerActivity(customer.id).first()
        assertTrue(activities.any { it.description.contains("moved from 'Enquiry' to 'Quotation'") })

        val archiveRes = repository.setCustomerArchived(customer.id, true)
        assertTrue(archiveRes.isSuccess)
        assertTrue(repository.observeCustomer(customer.id).first()!!.isArchived)
    }

    @Test
    fun `indian rupee formatting matches lakhs notation`() {
        assertEquals("₹1,00,000", Formatters.formatCurrency(100000.0, "₹", "INR"))
        assertEquals("₹60,000", Formatters.formatCurrency(60000.0, "₹", "INR"))
        assertEquals("5 kW", Formatters.formatCapacityKw(5.0))
        assertEquals("3.5 kW", Formatters.formatCapacityKw(3.5))
    }
}
