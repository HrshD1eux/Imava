package com.hrshd1eux.imava.core.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.hrshd1eux.imava.data.database.MediaMetadataEntity
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.repository.MediaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MetadataAutoBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockPrefs = mockk<SharedPreferences>(relaxed = true)
    private val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val mockRepository = mockk<MediaRepository>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Environment::class)
        every { Environment.getExternalStoragePublicDirectory(any()) } returns tempFolder.root

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockPrefs.getBoolean("auto_metadata_backup_enabled", true) } returns true
        every { mockPrefs.getBoolean("has_prompted_restore_backup", false) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testPerformBackupAndDetectBackup_success() = runBlocking {
        val testEntities = listOf(
            MediaMetadataEntity(
                mediaId = 1L,
                originalPath = "/storage/emulated/0/DCIM/Camera/IMG_01.jpg",
                tags = "vacation,beach",
                isFavorite = true
            ),
            MediaMetadataEntity(
                mediaId = 2L,
                originalPath = "/storage/emulated/0/DCIM/Camera/IMG_02.jpg",
                tags = "family",
                isFavorite = false
            ),
            MediaMetadataEntity(
                mediaId = 3L,
                originalPath = "/storage/emulated/0/DCIM/Camera/IMG_03.jpg",
                tags = "",
                isFavorite = false // Should be skipped in backup
            )
        )

        val backupResult = MetadataAutoBackupManager.performBackup(mockContext, testEntities)
        assertTrue(backupResult)

        val detected = MetadataAutoBackupManager.detectBackup(mockContext)
        assertNotNull(detected)
        assertEquals(2, detected!!.tagCount)
        assertEquals(1, detected.favoriteCount)
    }

    @Test
    fun testRestoreBackup_restoresTagsAndFavoritesByPath() = runBlocking {
        val testEntities = listOf(
            MediaMetadataEntity(
                mediaId = 100L,
                originalPath = "/storage/emulated/0/DCIM/Camera/IMG_RESTORE.jpg",
                tags = "travel,mountains",
                isFavorite = true
            )
        )

        // Perform initial backup
        MetadataAutoBackupManager.performBackup(mockContext, testEntities)

        // Mock live media in repository having a new mediaId (e.g. 555L) but matching originalPath
        val mockLiveItem = MediaItem.Photo(
            id = 555L,
            uri = mockk(relaxed = true),
            path = "/storage/emulated/0/DCIM/Camera/IMG_RESTORE.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1700000000000L,
            size = 2048L,
            width = 1920,
            height = 1080,
            bucketId = 1L,
            bucketName = "Camera"
        )

        coEvery { mockRepository.getAllMediaItemsDirect() } returns listOf(mockLiveItem)
        coEvery { mockRepository.getAllMetadata() } returns emptyList()

        val restoreResult = MetadataAutoBackupManager.restoreBackup(mockContext, mockRepository)
        assertEquals(1, restoreResult.restoredTagsCount)
        assertEquals(1, restoreResult.restoredFavoritesCount)
        assertEquals(1, restoreResult.totalRestored)

        coVerify { mockRepository.restoreMetadata(match { list ->
            list.size == 1 && list[0].mediaId == 555L && list[0].tags == "mountains,travel" && list[0].isFavorite
        }) }
    }
}
