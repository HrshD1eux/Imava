package com.hrshd1eux.imava.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hrshd1eux.imava.core.di.DatabaseModule

@Database(entities = [MediaMetadataEntity::class, MediaOcrEntity::class, WebDavSyncedEntity::class, ImavaDropHistoryEntity::class, DiscoveredMediaEntity::class], version = 11, exportSchema = true)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun metadataDao(): MetadataDao
    abstract fun ocrDao(): OcrDao
    abstract fun webDavSyncedDao(): WebDavSyncedDao
    abstract fun imavaDropHistoryDao(): ImavaDropHistoryDao
    abstract fun discoveredMediaDao(): DiscoveredMediaDao

    companion object {
        @Volatile
        private var INSTANCE: GalleryDatabase? = null

        private fun buildRoom(context: Context): GalleryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GalleryDatabase::class.java,
                "private_gallery.db"
            )
            .addMigrations(
                DatabaseModule.MIGRATION_2_3,
                DatabaseModule.MIGRATION_3_4,
                DatabaseModule.MIGRATION_4_5,
                DatabaseModule.MIGRATION_5_6,
                DatabaseModule.MIGRATION_6_7,
                DatabaseModule.MIGRATION_7_8,
                DatabaseModule.MIGRATION_6_8,
                DatabaseModule.MIGRATION_8_9,
                DatabaseModule.MIGRATION_9_10,
                DatabaseModule.MIGRATION_10_11
            )
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        }

        fun getInstance(context: Context): GalleryDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!
                var db = buildRoom(context)
                try {
                    // Eagerly execute and validate migrations inside try-catch to prevent launch crashes
                    db.openHelper.writableDatabase
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { db.close() } catch (_: Exception) {}
                    try { context.deleteDatabase("private_gallery.db") } catch (_: Exception) {}
                    db = buildRoom(context)
                    try { db.openHelper.writableDatabase } catch (_: Exception) {}
                }
                db.also { INSTANCE = it }
            }
        }
    }
}
