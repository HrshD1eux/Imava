package com.hrshd1eux.imava.ui

import android.app.Activity
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.repository.MediaRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.SavedStateHandle

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mockk<MediaRepository>(relaxed = true)
    private val mockApplication = mockk<android.app.Application>(relaxed = true)
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.mockkStatic(android.widget.Toast::class)
        val mockToast = mockk<android.widget.Toast>(relaxed = true)
        every { android.widget.Toast.makeText(any(), any<CharSequence>(), any()) } returns mockToast
        every { android.widget.Toast.makeText(any(), any<Int>(), any()) } returns mockToast
        
        every { mockApplication.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        every { mockRepository.getFavoriteMediaFlow() } returns flowOf(emptyList())
        every { mockRepository.getHiddenMediaFlow() } returns flowOf(emptyList())
        every { mockRepository.getTrashedMediaFlow() } returns flowOf(emptyList())
        every { mockRepository.observeMediaChanges() } returns kotlinx.coroutines.flow.emptyFlow()
        every { mockRepository.getMediaFlow(any(), any(), any()) } returns flowOf(emptyList())
        coEvery { mockRepository.loadMediaPaged(any(), any(), any(), any(), any(), any()) } returns emptyList()
        every { mockRepository.evictSecondaryDiskItem(any(), any()) } returns Unit
        
        viewModel = MainViewModel(mockApplication, mockRepository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        io.mockk.unmockkStatic(android.widget.Toast::class)
        Dispatchers.resetMain()
    }

    @Test
    fun testHandleActivityResult_deleteConfirmed_executesDBDeletion() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val mockItem = MediaItem.Photo(
            id = 55L,
            uri = mockUri,
            path = "/original/photo.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 100L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Camera"
        )

        viewModel.pendingActionItem = mockItem

        viewModel.handleActivityResult(1001, Activity.RESULT_OK)
        
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteMetadataPermanently(55L) }
        assertNull(viewModel.pendingActionItem)
    }

    @Test
    fun testHandleActivityResult_deleteCanceled_doesNotExecuteDBDeletion() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val mockItem = MediaItem.Photo(
            id = 55L,
            uri = mockUri,
            path = "/original/photo.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 100L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Camera"
        )

        viewModel.pendingActionItem = mockItem

        viewModel.handleActivityResult(1001, Activity.RESULT_CANCELED)
        
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.deleteMetadataPermanently(any()) }
        assertNull(viewModel.pendingActionItem)
    }

    @Test
    fun testHandleActivityResult_vaultHideCanceled_performsRollback() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val mockItem = MediaItem.Photo(
            id = 55L,
            uri = mockUri,
            path = "/original/photo.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 100L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Camera"
        )

        viewModel.pendingActionItem = mockItem

        viewModel.handleActivityResult(1004, Activity.RESULT_CANCELED)
        
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteMetadataPermanently(55L) }
        assertNull(viewModel.pendingActionItem)
    }

    @Test
    fun testLoadNextPage_loadsCorrectly() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val item1 = MediaItem.Photo(1L, mockUri, "/path1.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        
        val firstPageList = List(200) { item1 }
        coEvery { mockRepository.loadMediaPaged(limit = any(), offset = any(), bucketId = any(), bucketIds = any(), sortOrder = any()) } returns firstPageList

        viewModel.loadNextPage()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(200, viewModel.mediaItems.value.size)
        assertEquals(1L, viewModel.mediaItems.value[0].id)
    }

    @Test
    fun testDeleteSelectedMedia_resolvesSelectedItemsByIdBeyondFirst200() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val itemBeyond200 = MediaItem.Photo(999L, mockUri, "/path999.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        
        coEvery { mockRepository.getMediaByIds(setOf(999L)) } returns listOf(itemBeyond200)

        val mockContext = mockk<android.content.Context>(relaxed = true)

        viewModel.selectionState.select(999L)

        viewModel.deleteSelectedMedia(mockContext)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.getMediaByIds(setOf(999L)) }
    }

    @Test
    fun testSearchQuery_triggersRepositorySearch() = runTest(testDispatcher) {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val searchResultItem = MediaItem.Photo(500L, mockUri, "/path/vacation.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        
        coEvery { mockRepository.searchMedia("vacation") } returns listOf(searchResultItem)

        val job = backgroundScope.launch { viewModel.searchResults.collect {} }

        viewModel.setSearchQuery("vacation")
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.searchMedia("vacation") }
        assertEquals(1, viewModel.searchResults.value.size)
        assertEquals(500L, viewModel.searchResults.value[0].id)

        viewModel.setSearchQuery("")
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.searchResults.value.size)

        job.cancel()
    }

    @Test
    fun testDismissMemoriesFor24Hours_setsTimestamp() = runTest(testDispatcher) {
        assertEquals(0L, viewModel.memoriesDismissedTimestamp.value)
        viewModel.dismissMemoriesFor24Hours()
        org.junit.Assert.assertTrue(viewModel.memoriesDismissedTimestamp.value > 0L)
    }

    @Test
    fun testStartupScreen_albumsConfiguration_initializesToAlbums() {
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        every { mockApp.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString("default_start_screen", "timeline") } returns "albums"

        val vm = MainViewModel(mockApp, mockRepository, SavedStateHandle())
        assertEquals(Screen.Albums, vm.currentScreen)
    }

    @Test
    fun testStartupScreen_lastActiveConfiguration_readsLastActiveScreen() {
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        every { mockApp.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString("default_start_screen", "timeline") } returns "last_active"
        every { mockPrefs.getString("last_active_screen", "photos") } returns "albums"

        val vm = MainViewModel(mockApp, mockRepository, SavedStateHandle())
        assertEquals(Screen.Albums, vm.currentScreen)
    }

    @Test
    fun testStartupScreen_screenChange_persistsLastActiveScreen() {
        val mockEditor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        every { mockApp.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor

        val vm = MainViewModel(mockApp, mockRepository, SavedStateHandle())
        vm.currentScreen = Screen.Albums

        io.mockk.verify { mockEditor.putString("last_active_screen", "albums") }
    }

    @Test
    fun testAutoDeepScan_preferenceToggled_persistsValue() {
        val mockEditor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        every { mockApp.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor

        val vm = MainViewModel(mockApp, mockRepository, SavedStateHandle())
        vm.isAutoDeepScanEnabled = false

        io.mockk.verify { mockEditor.putBoolean("auto_deep_scan", false) }
    }

    @Test
    fun testScanSecondaryMediaDirectories_delegatesAndStoresResults() = kotlinx.coroutines.runBlocking {
        val mockEditor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        every { mockApp.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor

        io.mockk.coEvery { mockRepository.scanSecondaryMediaDirectories() } returns com.hrshd1eux.imava.data.media.DeepScanResult(newCount = 5, totalCount = 69)

        val vm = MainViewModel(mockApp, mockRepository, SavedStateHandle())
        val result = vm.scanSecondaryMediaDirectories()

        org.junit.Assert.assertEquals(5, result.newCount)
        org.junit.Assert.assertEquals(69, result.totalCount)
        org.junit.Assert.assertEquals(69, vm.lastDeepScanCount.value)
        org.junit.Assert.assertEquals(5, vm.lastDeepScanNewCount.value)
        io.mockk.verify { mockEditor.putInt("last_deep_scan_count", 69) }
        io.mockk.verify { mockEditor.putInt("last_deep_scan_new_count", 5) }
    }

    @Test
    fun testOpenExternalMedia_setsExternalViewIntentAndActiveItem() = kotlinx.coroutines.runBlocking {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        every { mockUri.scheme } returns "file"
        every { mockUri.path } returns "/storage/emulated/0/DCIM/Camera/external.jpg"
        every { mockUri.lastPathSegment } returns "external.jpg"
        every { mockUri.toString() } returns "file:///storage/emulated/0/DCIM/Camera/external.jpg"

        viewModel.openExternalMedia(mockContext, mockUri, "image/jpeg")
        for (i in 1..50) {
            testDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.activeMediaItem != null) break
            Thread.sleep(20)
        }

        org.junit.Assert.assertTrue(viewModel.isExternalViewIntent)
        val active = viewModel.activeMediaItem
        org.junit.Assert.assertNotNull(active)
        org.junit.Assert.assertEquals("/storage/emulated/0/DCIM/Camera/external.jpg", active?.path)
        org.junit.Assert.assertEquals("image/jpeg", active?.mimeType)
    }

    @Test
    fun testMatchesFilter_rawExtensionsAndMimes() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)

        val rawDng = MediaItem.Photo(id = 1L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/DSC001.dng", mimeType = "image/dng", dateTaken = 1000L, size = 50000L, width = 4000, height = 3000, bucketId = 1L, bucketName = "Camera")
        val rawCr3 = MediaItem.Photo(id = 2L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/IMG_01.CR3", mimeType = "image/x-canon-cr3", dateTaken = 1000L, size = 50000L, width = 4000, height = 3000, bucketId = 1L, bucketName = "Camera")
        val rawNef = MediaItem.Photo(id = 3L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/NIK_01.NEF", mimeType = "image/x-nikon-nef", dateTaken = 1000L, size = 50000L, width = 4000, height = 3000, bucketId = 1L, bucketName = "Camera")
        val rawArw = MediaItem.Photo(id = 4L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/SONY_01.arw", mimeType = "image/x-sony-arw", dateTaken = 1000L, size = 50000L, width = 4000, height = 3000, bucketId = 1L, bucketName = "Camera")
        val standardJpg = MediaItem.Photo(id = 5L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/NORMAL.jpg", mimeType = "image/jpeg", dateTaken = 1000L, size = 2000L, width = 1920, height = 1080, bucketId = 1L, bucketName = "Camera")

        org.junit.Assert.assertTrue(viewModel.matchesFilter(rawDng, MediaFilterType.RAW))
        org.junit.Assert.assertTrue(viewModel.matchesFilter(rawCr3, MediaFilterType.RAW))
        org.junit.Assert.assertTrue(viewModel.matchesFilter(rawNef, MediaFilterType.RAW))
        org.junit.Assert.assertTrue(viewModel.matchesFilter(rawArw, MediaFilterType.RAW))
        org.junit.Assert.assertFalse(viewModel.matchesFilter(standardJpg, MediaFilterType.RAW))
    }

    @Test
    fun testMatchesFilter_screenshotsAndGifs() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)

        val screenshot1 = MediaItem.Photo(id = 1L, uri = mockUri, path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_1.png", mimeType = "image/png", dateTaken = 1000L, size = 2000L, width = 1080, height = 2400, bucketId = 2L, bucketName = "Screenshots")
        val screenshot2 = MediaItem.Photo(id = 2L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/screenshot_2024.jpg", mimeType = "image/jpeg", dateTaken = 1000L, size = 2000L, width = 1080, height = 2400, bucketId = 1L, bucketName = "Camera")
        val normalPhoto = MediaItem.Photo(id = 3L, uri = mockUri, path = "/storage/emulated/0/DCIM/Camera/IMG_2024.jpg", mimeType = "image/jpeg", dateTaken = 1000L, size = 2000L, width = 1920, height = 1080, bucketId = 1L, bucketName = "Camera")
        val gifItem = MediaItem.Photo(id = 4L, uri = mockUri, path = "/storage/emulated/0/Download/funny.gif", mimeType = "image/gif", dateTaken = 1000L, size = 500L, width = 500, height = 500, bucketId = 3L, bucketName = "Download")

        org.junit.Assert.assertTrue(viewModel.matchesFilter(screenshot1, MediaFilterType.SCREENSHOTS))
        org.junit.Assert.assertTrue(viewModel.matchesFilter(screenshot2, MediaFilterType.SCREENSHOTS))
        org.junit.Assert.assertFalse(viewModel.matchesFilter(normalPhoto, MediaFilterType.SCREENSHOTS))

        org.junit.Assert.assertTrue(viewModel.matchesFilter(gifItem, MediaFilterType.GIFS))
        org.junit.Assert.assertFalse(viewModel.matchesFilter(normalPhoto, MediaFilterType.GIFS))
    }

    @Test
    fun testAdvanceActiveMediaItem_whenMiddleItemDeleted_advancesToNextItemNotFirst() = runTest(testDispatcher) {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val item1 = MediaItem.Photo(10L, mockUri, "/path10.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item2 = MediaItem.Photo(20L, mockUri, "/path20.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item3 = MediaItem.Photo(30L, mockUri, "/path30.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item4 = MediaItem.Photo(40L, mockUri, "/path40.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item5 = MediaItem.Photo(50L, mockUri, "/path50.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")

        viewModel.setCustomViewerMediaItems(listOf(item1, item2, item3, item4, item5))
        for (i in 1..50) {
            testDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.viewerMediaItems.value.size == 5) break
            Thread.sleep(20)
        }

        viewModel.activeMediaItem = item3
        viewModel.advanceActiveMediaItem(deletedItem = item3)

        // Item 3 was at index 2; next item is item 4 at index 3, NOT item 1 at index 0!
        assertEquals(40L, viewModel.activeMediaItem?.id)
    }

    @Test
    fun testAdvanceActiveMediaItem_whenLastItemDeleted_advancesToPreviousItemNotFirst() = runTest(testDispatcher) {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val item1 = MediaItem.Photo(10L, mockUri, "/path10.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item2 = MediaItem.Photo(20L, mockUri, "/path20.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item3 = MediaItem.Photo(30L, mockUri, "/path30.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")

        viewModel.setCustomViewerMediaItems(listOf(item1, item2, item3))
        for (i in 1..50) {
            testDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.viewerMediaItems.value.size == 3) break
            Thread.sleep(20)
        }

        viewModel.activeMediaItem = item3
        viewModel.advanceActiveMediaItem(deletedItem = item3)

        // Item 3 was the last item; previous item is item 2, NOT item 1!
        assertEquals(20L, viewModel.activeMediaItem?.id)
    }

    @Test
    fun testAdvanceActiveMediaItem_whenExplicitTargetNextItemProvided_directlyAdvances() {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val item2 = MediaItem.Photo(20L, mockUri, "/path20.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")
        val item3 = MediaItem.Photo(30L, mockUri, "/path30.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")

        viewModel.activeMediaItem = item2
        viewModel.advanceActiveMediaItem(deletedItem = item2, targetNextItem = item3)

        assertEquals(30L, viewModel.activeMediaItem?.id)
    }

    @Test
    fun testAdvanceActiveMediaItem_whenSoleItemDeleted_setsActiveItemNull() = runTest(testDispatcher) {
        val mockUri = mockk<android.net.Uri>(relaxed = true)
        val item1 = MediaItem.Photo(10L, mockUri, "/path10.jpg", "image/jpeg", 1000L, 100L, 100, 100, false, false, false, 1L, "Camera")

        viewModel.setCustomViewerMediaItems(listOf(item1))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.activeMediaItem = item1
        viewModel.advanceActiveMediaItem(deletedItem = item1)

        assertNull(viewModel.activeMediaItem)
    }

    @Test
    fun testBatchOperationProgress_updateAndClear() {
        assertNull(viewModel.batchOperationProgress.value)

        viewModel.updateBatchProgress(title = "Moving media…", current = 5, total = 20)
        val progress = viewModel.batchOperationProgress.value
        org.junit.Assert.assertNotNull(progress)
        assertEquals("Moving media…", progress?.title)
        assertEquals(5, progress?.current)
        assertEquals(20, progress?.total)

        viewModel.clearBatchProgress()
        assertNull(viewModel.batchOperationProgress.value)
    }
}
