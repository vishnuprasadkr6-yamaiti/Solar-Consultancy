package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ChecklistProjectStatusRow(
    val projectId: String,
    val totalItems: Int,
    val completedItems: Int
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun observeCustomerById(customerId: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countCustomers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun observeProjectsForCustomer(customerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun countProjects(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface ProjectChecklistDao {
    @Query("SELECT * FROM project_checklist ORDER BY projectId ASC, stepOrder ASC")
    fun observeAllChecklistItems(): Flow<List<ProjectChecklist>>

    @Query(
        "SELECT c.* FROM project_checklist c " +
            "INNER JOIN projects p ON p.id = c.projectId " +
            "WHERE p.customerId = :customerId " +
            "ORDER BY c.projectId ASC, c.stepOrder ASC"
    )
    fun observeChecklistsForCustomer(customerId: String): Flow<List<ProjectChecklist>>

    @Query("SELECT * FROM project_checklist WHERE projectId = :projectId ORDER BY stepOrder ASC")
    suspend fun getChecklistForProject(projectId: String): List<ProjectChecklist>

    @Query("SELECT * FROM project_checklist WHERE id = :itemId")
    suspend fun getChecklistItemById(itemId: String): ProjectChecklist?

    @Query(
        "SELECT p.id AS projectId, " +
            "COUNT(c.id) AS totalItems, " +
            "SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) AS completedItems " +
            "FROM projects p " +
            "LEFT JOIN project_checklist c ON c.projectId = p.id " +
            "WHERE p.status != 'ARCHIVED' " +
            "GROUP BY p.id"
    )
    fun observeChecklistCompletionSummary(): Flow<List<ChecklistProjectStatusRow>>

    @Query("SELECT COUNT(*) FROM project_checklist")
    suspend fun countChecklistItems(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChecklistItems(items: List<ProjectChecklist>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChecklistItem(item: ProjectChecklist)

    @Update
    suspend fun updateChecklistItem(item: ProjectChecklist)

    @Query("DELETE FROM project_checklist WHERE id = :itemId")
    suspend fun deleteChecklistItemById(itemId: String)

    @Query("DELETE FROM project_checklist")
    suspend fun deleteAllChecklistItems()
}

@Dao
interface ProjectPhotoDao {
    @Query(
        "SELECT ph.* FROM project_photos ph " +
            "INNER JOIN projects p ON p.id = ph.projectId " +
            "WHERE p.customerId = :customerId " +
            "ORDER BY ph.capturedAt DESC"
    )
    fun observePhotosForCustomer(customerId: String): Flow<List<ProjectPhoto>>

    @Query("SELECT * FROM project_photos WHERE projectId = :projectId ORDER BY capturedAt DESC")
    fun observePhotosForProject(projectId: String): Flow<List<ProjectPhoto>>

    @Query("SELECT * FROM project_photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: String): ProjectPhoto?

    @Query("SELECT COUNT(*) FROM project_photos")
    suspend fun countPhotos(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhoto(photo: ProjectPhoto)

    @Query("DELETE FROM project_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: String)

    @Query("DELETE FROM project_photos")
    suspend fun deleteAllPhotos()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentActivity(limit: Int = 50): Flow<List<ActivityLogEntity>>

    @Query(
        "SELECT * FROM activity_log " +
            "WHERE customerId = :customerId OR (entityType = 'CUSTOMER' AND entityId = :customerId) " +
            "ORDER BY timestamp DESC"
    )
    fun observeActivityForCustomer(customerId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT COUNT(*) FROM activity_log")
    suspend fun countActivities(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivity(activity: ActivityLogEntity)

    @Query("DELETE FROM activity_log")
    suspend fun deleteAllActivities()
}

@Dao
interface SequenceCounterDao {
    @Query("SELECT * FROM sequence_counters WHERE prefix = :prefix")
    suspend fun getCounter(prefix: String): SequenceCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: SequenceCounterEntity)

    @Query("DELETE FROM sequence_counters")
    suspend fun deleteAllCounters()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}
