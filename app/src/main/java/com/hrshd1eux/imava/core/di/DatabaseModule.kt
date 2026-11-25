package com.hrshd1eux.imava.core.di

import android.content.Context
import androidx.room.Room
import com.hrshd1eux.imava.data.database.GalleryDatabase
import com.hrshd1eux.imava.data.database.MetadataDao
import com.hrshd1eux.imava.data.repository.MediaRepository
import com.hrshd1eux.imava.data.repository.MediaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_isFavorite ON media_metadata(isFavorite)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_isHidden ON media_metadata(isHidden)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_isTrashed ON media_metadata(isTrashed)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_dateTaken ON media_metadata(dateTaken)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_bucketId ON media_metadata(bucketId)")
        }
    }

    val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE media_metadata ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS media_ocr (mediaId INTEGER PRIMARY KEY NOT NULL, ocrText TEXT NOT NULL)")
        }
    }

    val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS media_ocr")
        }
    }

    val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_media_ocr_mediaId")
            db.execSQL("CREATE TABLE IF NOT EXISTS media_ocr (mediaId INTEGER PRIMARY KEY NOT NULL, ocrText TEXT NOT NULL, scannedAt INTEGER NOT NULL DEFAULT 0)")
        }
    }

    val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_media_ocr_mediaId")
            db.execSQL("CREATE TABLE IF NOT EXISTS media_ocr_new (mediaId INTEGER PRIMARY KEY NOT NULL, ocrText TEXT NOT NULL, scannedAt INTEGER NOT NULL)")
            try {
                db.execSQL("INSERT OR IGNORE INTO media_ocr_new (mediaId, ocrText, scannedAt) SELECT mediaId, ocrText, scannedAt FROM media_ocr")
            } catch (_: Exception) {}
            db.execSQL("DROP TABLE IF EXISTS media_ocr")
            db.execSQL("ALTER TABLE media_ocr_new RENAME TO media_ocr")
        }
    }

    val MIGRATION_6_8 = object : androidx.room.migration.Migration(6, 8) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS media_ocr")
            db.execSQL("CREATE TABLE IF NOT EXISTS media_ocr (mediaId INTEGER PRIMARY KEY NOT NULL, ocrText TEXT NOT NULL, scannedAt INTEGER NOT NULL)")
        }
    }

    val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS webdav_synced_media (mediaId INTEGER PRIMARY KEY NOT NULL, syncedAt INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_webdav_synced_media_mediaId ON webdav_synced_media(mediaId)")
        }
    }

    val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `imava_drop_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `fileSizeBytes` INTEGER NOT NULL,
                    `direction` TEXT NOT NULL,
                    `clientIp` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `mediaUri` TEXT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_imava_drop_history_timestamp` ON `imava_drop_history`(`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_imava_drop_history_direction` ON `imava_drop_history`(`direction`)")
        }
    }

    val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `discovered_media` (
                    `id` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `dateTaken` INTEGER NOT NULL,
                    `size` INTEGER NOT NULL,
                    `width` INTEGER NOT NULL,
                    `height` INTEGER NOT NULL,
                    `durationMs` INTEGER NOT NULL,
                    `bucketId` INTEGER NOT NULL,
                    `bucketName` TEXT NOT NULL,
                    `isVideo` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_discovered_media_path` ON `discovered_media`(`path`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discovered_media_dateTaken` ON `discovered_media`(`dateTaken`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discovered_media_bucketId` ON `discovered_media`(`bucketId`)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GalleryDatabase {
        return GalleryDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMetadataDao(db: GalleryDatabase): MetadataDao {
        return db.metadataDao()
    }

    @Provides
    @Singleton
    fun provideOcrDao(db: GalleryDatabase): com.hrshd1eux.imava.data.database.OcrDao {
        return db.ocrDao()
    }

    @Provides
    @Singleton
    fun provideWebDavSyncedDao(db: GalleryDatabase): com.hrshd1eux.imava.data.database.WebDavSyncedDao {
        return db.webDavSyncedDao()
    }

    @Provides
    @Singleton
    fun provideImavaDropHistoryDao(db: GalleryDatabase): com.hrshd1eux.imava.data.database.ImavaDropHistoryDao {
        return db.imavaDropHistoryDao()
    }

    @Provides
    @Singleton
    fun provideDiscoveredMediaDao(db: GalleryDatabase): com.hrshd1eux.imava.data.database.DiscoveredMediaDao {
        return db.discoveredMediaDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
