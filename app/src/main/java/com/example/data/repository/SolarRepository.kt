package com.example.data.repository

import android.content.Context
import android.util.Patterns
import androidx.room.withTransaction
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.ProjectEntity
import com.example.data.local.SequenceCounterEntity
import com.example.data.local.SettingsEntity
import com.example.data.model.DashboardPreferences
import com.example.data.model.DashboardProjectFilter
import com.example.data.model.DashboardSectionId
import com.example.data.model.DashboardTimeFilter
import com.example.data.model.DatabaseStats
import com.example.data.model.DefaultStages
import com.example.data.model.ProjectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

data class CustomerInput(
    val fullName: String,
    val primaryPhone: String,
    val secondaryPhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val postalCode: String = "",
    val notes: String = ""
)

data class ProjectInput(
    val customerId: String,
    val name: String,
    val capacityKw: Double,
    val projectType: String,
    val installationLocation: String,
    val assignedPerson: String = "",
    val currentStage: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val expectedCompletionDate: Long? = null,
    val quotationAmount: Double = 0.0,
    val notes: String = ""
)

class SolarRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val preferencesStore: PreferencesStore
) {
    private val customerDao = db.customerDao()
    private val projectDao = db.projectDao()
    private val activityDao = db.activityLogDao()
    private val counterDao = db.sequenceCounterDao()
    private val settingsDao = db.settingsDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.observeAllCustomers()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()
    val recentActivity: Flow<List<ActivityLogEntity>> = activityDao.observeRecentActivity(60)
    val dashboardPreferences: Flow<DashboardPreferences> = preferencesStore.dashboardPreferences

    val settings: Flow<SettingsEntity> = settingsDao.observeSettings().map {
        it ?: SettingsEntity()
    }

    fun observeCustomer(customerId: String): Flow<CustomerEntity?> =
        customerDao.observeCustomerById(customerId)

    fun observeCustomerProjects(customerId: String): Flow<List<ProjectEntity>> =
        projectDao.observeProjectsForCustomer(customerId)

    fun observeCustomerActivity(customerId: String): Flow<List<ActivityLogEntity>> =
        activityDao.observeActivityForCustomer(customerId)

    suspend fun ensureDefaultSettings() {
        withContext(Dispatchers.IO) {
            if (settingsDao.getSettings() == null) {
                settingsDao.upsertSettings(SettingsEntity())
            }
        }
    }

    private suspend fun nextFormattedId(prefix: String): String {
        val current = counterDao.getCounter(prefix)?.nextValue ?: 1
        counterDao.upsertCounter(SequenceCounterEntity(prefix = prefix, nextValue = current + 1))
        return String.format(Locale.ENGLISH, "%s-%05d", prefix, current)
    }

    private suspend fun logActivityInternal(
        entityType: String,
        entityId: String,
        customerId: String?,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val actId = nextFormattedId("ACT")
        activityDao.insertActivity(
            ActivityLogEntity(
                id = actId,
                entityType = entityType,
                entityId = entityId,
                customerId = customerId,
                description = description,
                timestamp = timestamp
            )
        )
    }

    private fun validateCustomer(input: CustomerInput): String? {
        if (input.fullName.trim().length < 2) {
            return "Full name must be at least 2 characters."
        }
        val digits = input.primaryPhone.filter { it.isDigit() }
        if (digits.length < 7 || digits.length > 15) {
            return "Enter a valid primary phone number (7–15 digits)."
        }
        val secondaryDigits = input.secondaryPhone.filter { it.isDigit() }
        if (input.secondaryPhone.isNotBlank() && (secondaryDigits.length < 7 || secondaryDigits.length > 15)) {
            return "Secondary phone number must contain 7–15 digits."
        }
        val emailTrimmed = input.email.trim()
        if (emailTrimmed.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            return "Enter a valid email address."
        }
        return null
    }

    private fun validateProject(input: ProjectInput): String? {
        if (input.name.trim().length < 2) {
            return "Project name or reference is required."
        }
        if (input.capacityKw <= 0.0 || input.capacityKw.isNaN() || input.capacityKw.isInfinite()) {
            return "Solar capacity (kW) must be greater than 0."
        }
        if (input.installationLocation.trim().isEmpty()) {
            return "Installation location is required."
        }
        if (input.quotationAmount < 0.0 || input.quotationAmount.isNaN() || input.quotationAmount.isInfinite()) {
            return "Quotation amount cannot be negative."
        }
        if (input.currentStage.trim().isEmpty()) {
            return "Project stage is required."
        }
        return null
    }

    suspend fun createCustomer(input: CustomerInput): Result<CustomerEntity> = withContext(Dispatchers.IO) {
        val error = validateCustomer(input)
        if (error != null) return@withContext Result.failure(IllegalArgumentException(error))

        runCatching {
            db.withTransaction {
                val now = System.currentTimeMillis()
                val id = nextFormattedId("CUS")
                val entity = CustomerEntity(
                    id = id,
                    fullName = input.fullName.trim(),
                    primaryPhone = input.primaryPhone.trim(),
                    secondaryPhone = input.secondaryPhone.trim(),
                    email = input.email.trim(),
                    address = input.address.trim(),
                    city = input.city.trim(),
                    postalCode = input.postalCode.trim(),
                    notes = input.notes.trim(),
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                )
                customerDao.insertCustomer(entity)
                logActivityInternal(
                    entityType = "CUSTOMER",
                    entityId = id,
                    customerId = id,
                    description = "Customer $id (${entity.fullName}) created.",
                    timestamp = now
                )
                entity
            }
        }
    }

    suspend fun updateCustomer(customerId: String, input: CustomerInput): Result<CustomerEntity> =
        withContext(Dispatchers.IO) {
            val error = validateCustomer(input)
            if (error != null) return@withContext Result.failure(IllegalArgumentException(error))

            runCatching {
                db.withTransaction {
                    val existing = customerDao.getCustomerById(customerId)
                        ?: throw IllegalStateException("Customer $customerId not found.")
                    val now = System.currentTimeMillis()
                    val updated = existing.copy(
                        fullName = input.fullName.trim(),
                        primaryPhone = input.primaryPhone.trim(),
                        secondaryPhone = input.secondaryPhone.trim(),
                        email = input.email.trim(),
                        address = input.address.trim(),
                        city = input.city.trim(),
                        postalCode = input.postalCode.trim(),
                        notes = input.notes.trim(),
                        updatedAt = now
                    )
                    customerDao.updateCustomer(updated)
                    logActivityInternal(
                        entityType = "CUSTOMER",
                        entityId = customerId,
                        customerId = customerId,
                        description = "Customer $customerId (${updated.fullName}) profile updated.",
                        timestamp = now
                    )
                    updated
                }
            }
        }

    suspend fun setCustomerArchived(customerId: String, archived: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.withTransaction {
                    val existing = customerDao.getCustomerById(customerId)
                        ?: throw IllegalStateException("Customer $customerId not found.")
                    val now = System.currentTimeMillis()
                    customerDao.updateCustomer(existing.copy(isArchived = archived, updatedAt = now))
                    val action = if (archived) "archived" else "restored from archive"
                    logActivityInternal(
                        entityType = "CUSTOMER",
                        entityId = customerId,
                        customerId = customerId,
                        description = "Customer $customerId (${existing.fullName}) $action.",
                        timestamp = now
                    )
                }
            }
        }

    suspend fun createProject(input: ProjectInput): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        val error = validateProject(input)
        if (error != null) return@withContext Result.failure(IllegalArgumentException(error))

        runCatching {
            db.withTransaction {
                val customer = customerDao.getCustomerById(input.customerId)
                    ?: throw IllegalStateException("Customer ${input.customerId} does not exist.")
                val now = System.currentTimeMillis()
                val id = nextFormattedId("PRJ")
                val effectiveStatus = if (input.currentStage.equals("Completed", ignoreCase = true)) {
                    ProjectStatus.COMPLETED
                } else {
                    input.status
                }
                val project = ProjectEntity(
                    id = id,
                    customerId = input.customerId,
                    name = input.name.trim(),
                    capacityKw = input.capacityKw,
                    projectType = input.projectType.trim(),
                    installationLocation = input.installationLocation.trim(),
                    assignedPerson = input.assignedPerson.trim(),
                    currentStage = input.currentStage.trim(),
                    status = effectiveStatus.name,
                    expectedCompletionDate = input.expectedCompletionDate,
                    quotationAmount = input.quotationAmount,
                    notes = input.notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                projectDao.insertProject(project)
                customerDao.updateCustomer(customer.copy(updatedAt = now))
                logActivityInternal(
                    entityType = "PROJECT",
                    entityId = id,
                    customerId = customer.id,
                    description = "Project $id (${project.name}, ${project.capacityKw} kW) created for ${customer.fullName} at stage '${project.currentStage}'.",
                    timestamp = now
                )
                project
            }
        }
    }

    suspend fun updateProject(projectId: String, input: ProjectInput): Result<ProjectEntity> =
        withContext(Dispatchers.IO) {
            val error = validateProject(input)
            if (error != null) return@withContext Result.failure(IllegalArgumentException(error))

            runCatching {
                db.withTransaction {
                    val existing = projectDao.getProjectById(projectId)
                        ?: throw IllegalStateException("Project $projectId not found.")
                    val customer = customerDao.getCustomerById(existing.customerId)
                        ?: throw IllegalStateException("Parent customer ${existing.customerId} not found.")
                    val now = System.currentTimeMillis()
                    val updated = existing.copy(
                        name = input.name.trim(),
                        capacityKw = input.capacityKw,
                        projectType = input.projectType.trim(),
                        installationLocation = input.installationLocation.trim(),
                        assignedPerson = input.assignedPerson.trim(),
                        currentStage = input.currentStage.trim(),
                        status = input.status.name,
                        expectedCompletionDate = input.expectedCompletionDate,
                        quotationAmount = input.quotationAmount,
                        notes = input.notes.trim(),
                        updatedAt = now
                    )
                    projectDao.updateProject(updated)
                    customerDao.updateCustomer(customer.copy(updatedAt = now))
                    if (existing.currentStage != updated.currentStage) {
                        logActivityInternal(
                            entityType = "PROJECT",
                            entityId = projectId,
                            customerId = customer.id,
                            description = "Project $projectId (${updated.name}) stage changed from '${existing.currentStage}' to '${updated.currentStage}'.",
                            timestamp = now
                        )
                    } else {
                        logActivityInternal(
                            entityType = "PROJECT",
                            entityId = projectId,
                            customerId = customer.id,
                            description = "Project $projectId (${updated.name}) details updated.",
                            timestamp = now
                        )
                    }
                    updated
                }
            }
        }

    suspend fun updateProjectStage(
        projectId: String,
        newStage: String,
        newStatus: ProjectStatus? = null,
        stageNote: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedStage = newStage.trim()
        if (trimmedStage.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Stage cannot be empty."))
        }
        runCatching {
            db.withTransaction {
                val existing = projectDao.getProjectById(projectId)
                    ?: throw IllegalStateException("Project $projectId not found.")
                val customer = customerDao.getCustomerById(existing.customerId)
                val now = System.currentTimeMillis()
                val resolvedStatus = when {
                    newStatus != null -> newStatus.name
                    trimmedStage.equals("Completed", ignoreCase = true) -> ProjectStatus.COMPLETED.name
                    existing.status == ProjectStatus.COMPLETED.name &&
                        !trimmedStage.equals("Completed", ignoreCase = true) -> ProjectStatus.ACTIVE.name
                    else -> existing.status
                }
                projectDao.updateProject(
                    existing.copy(
                        currentStage = trimmedStage,
                        status = resolvedStatus,
                        updatedAt = now
                    )
                )
                if (customer != null) {
                    customerDao.updateCustomer(customer.copy(updatedAt = now))
                }
                val noteSuffix = if (stageNote.isNotBlank()) " Note: ${stageNote.trim()}" else ""
                val msg = if (existing.currentStage != trimmedStage) {
                    "Project $projectId (${existing.name}) moved from '${existing.currentStage}' to '$trimmedStage'.$noteSuffix"
                } else {
                    "Project $projectId (${existing.name}) status updated to '${ProjectStatus.valueOf(resolvedStatus).label}'.$noteSuffix"
                }
                logActivityInternal(
                    entityType = "PROJECT",
                    entityId = projectId,
                    customerId = existing.customerId,
                    description = msg,
                    timestamp = now
                )
            }
        }
    }

    suspend fun saveSettings(
        businessName: String,
        businessPhone: String,
        businessEmail: String,
        businessAddress: String,
        taxOrGstId: String,
        currencySymbol: String,
        currencyCode: String,
        stages: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (businessName.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Business name cannot be empty."))
        }
        val cleanStages = stages.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanStages.size < 2) {
            return@withContext Result.failure(IllegalArgumentException("At least 2 project stages are required."))
        }
        val cleanSymbol = currencySymbol.trim().ifEmpty { "₹" }
        val cleanCode = currencyCode.trim().uppercase(Locale.ENGLISH).ifEmpty { "INR" }

        runCatching {
            db.withTransaction {
                val now = System.currentTimeMillis()
                settingsDao.upsertSettings(
                    SettingsEntity(
                        id = 1,
                        businessName = businessName.trim(),
                        businessPhone = businessPhone.trim(),
                        businessEmail = businessEmail.trim(),
                        businessAddress = businessAddress.trim(),
                        taxOrGstId = taxOrGstId.trim(),
                        currencySymbol = cleanSymbol,
                        currencyCode = cleanCode,
                        stagesEncoded = DefaultStages.encodeStages(cleanStages),
                        updatedAt = now
                    )
                )
                logActivityInternal(
                    entityType = "SETTINGS",
                    entityId = "SETTINGS",
                    customerId = null,
                    description = "Business and project stage configuration updated.",
                    timestamp = now
                )
            }
        }
    }

    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val walFile = context.getDatabasePath("${AppDatabase.DATABASE_NAME}-wal")
        val shmFile = context.getDatabasePath("${AppDatabase.DATABASE_NAME}-shm")
        val sizeBytes = listOf(dbFile, walFile, shmFile)
            .filter { it.exists() }
            .sumOf { it.length() }

        DatabaseStats(
            customerCount = customerDao.countCustomers(),
            projectCount = projectDao.countProjects(),
            activityCount = activityDao.countActivities(),
            databaseSizeBytes = sizeBytes
        )
    }

    suspend fun resetAllApplicationData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.withTransaction {
                projectDao.deleteAllProjects()
                customerDao.deleteAllCustomers()
                activityDao.deleteAllActivities()
                counterDao.deleteAllCounters()
                settingsDao.upsertSettings(SettingsEntity())
            }
            preferencesStore.resetDashboardPreferences()
        }
    }

    suspend fun setDashboardProjectFilter(filter: DashboardProjectFilter) {
        preferencesStore.saveProjectFilter(filter)
    }

    suspend fun setDashboardTimeFilter(filter: DashboardTimeFilter) {
        preferencesStore.saveTimeFilter(filter)
    }

    suspend fun toggleDashboardSection(sectionId: DashboardSectionId) {
        preferencesStore.toggleSectionVisibility(sectionId)
    }

    suspend fun moveDashboardSection(sectionId: DashboardSectionId, moveUp: Boolean, currentOrder: List<DashboardSectionId>) {
        val mutable = currentOrder.toMutableList()
        val index = mutable.indexOf(sectionId)
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in mutable.indices) return
        mutable[index] = mutable[targetIndex]
        mutable[targetIndex] = sectionId
        preferencesStore.saveSectionOrder(mutable)
    }
}
