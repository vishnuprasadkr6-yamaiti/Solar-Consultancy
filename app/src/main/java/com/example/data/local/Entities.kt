package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.DefaultStages

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["fullName"]),
        Index(value = ["primaryPhone"]),
        Index(value = ["isArchived"]),
        Index(value = ["updatedAt"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String, // CUS-00001
    val fullName: String,
    val primaryPhone: String,
    val secondaryPhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val postalCode: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["currentStage"]),
        Index(value = ["updatedAt"])
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String, // PRJ-00001
    val customerId: String,
    val name: String,
    val capacityKw: Double,
    val projectType: String,
    val installationLocation: String,
    val assignedPerson: String = "",
    val currentStage: String,
    val status: String, // ACTIVE, COMPLETED, ARCHIVED
    val expectedCompletionDate: Long? = null,
    val quotationAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "project_checklist",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["isCompleted"])
    ]
)
data class ProjectChecklist(
    @PrimaryKey val id: String, // CHK-00001
    val projectId: String,
    val stepOrder: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    val updatedAt: Long
)

@Entity(
    tableName = "project_photos",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["capturedAt"])
    ]
)
data class ProjectPhoto(
    @PrimaryKey val id: String, // PHO-00001
    val projectId: String,
    val localUri: String, // Local FileProvider / file URI string only (never BLOBs)
    val caption: String = "",
    val capturedAt: Long
)

@Entity(
    tableName = "activity_log",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["timestamp"])
    ]
)
data class ActivityLogEntity(
    @PrimaryKey val id: String, // ACT-00001
    val entityType: String, // CUSTOMER, PROJECT, CHECKLIST, PHOTO, QUOTE, SETTINGS
    val entityId: String,
    val customerId: String? = null,
    val description: String,
    val timestamp: Long
)

@Entity(tableName = "sequence_counters")
data class SequenceCounterEntity(
    @PrimaryKey val prefix: String, // CUS, PRJ, CHK, PHO, ACT
    val nextValue: Int
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "Solar Consultancy",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessAddress: String = "",
    val taxOrGstId: String = "",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val stagesEncoded: String = DefaultStages.encodeStages(DefaultStages.STAGES),
    val updatedAt: Long = System.currentTimeMillis()
)
