package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CustomerEntity::class,
        ProjectEntity::class,
        ProjectChecklist::class,
        ProjectPhoto::class,
        ActivityLogEntity::class,
        SequenceCounterEntity::class,
        SettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectChecklistDao(): ProjectChecklistDao
    abstract fun projectPhotoDao(): ProjectPhotoDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun sequenceCounterDao(): SequenceCounterDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "solardesk_local.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_checklist` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `stepOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `notes` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_checklist_projectId` ON `project_checklist` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_checklist_isCompleted` ON `project_checklist` (`isCompleted`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_photos` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `localUri` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `capturedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_photos_projectId` ON `project_photos` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_photos_capturedAt` ON `project_photos` (`capturedAt`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
