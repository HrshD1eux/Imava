package com.hrshd1eux.imava.data.paging

import androidx.paging.PagingSource
import com.hrshd1eux.imava.data.media.MediaTypeFilter
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.repository.MediaRepository
import com.hrshd1eux.imava.ui.SortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPagingSourceTest {

    private val mockRepository = mockk<MediaRepository>(relaxed = true)

    @Test
    fun testLoad_pageZero_loadsInstantlyWithoutCallingGetTotalMediaCount() = runBlocking {
        val mockItems = listOf(
            mockk<MediaItem.Photo>(relaxed = true),
            mockk<MediaItem.Photo>(relaxed = true)
        )

        coEvery {
            mockRepository.loadMediaPaged(
                limit = 50,
                offset = 0,
                bucketId = null,
                bucketIds = null,
                sortOrder = SortOrder.NEWEST_FIRST,
                mediaType = MediaTypeFilter.ALL
            )
        } returns mockItems

        val pagingSource = MediaPagingSource(
            repository = mockRepository,
            bucketId = null,
            bucketIds = null,
            sortOrder = SortOrder.NEWEST_FIRST,
            mediaType = MediaTypeFilter.ALL
        )

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 50,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)
        assertTrue(result is PagingSource.LoadResult.Page)

        val page = result as PagingSource.LoadResult.Page<Int, MediaItem>
        assertEquals(2, page.data.size)
        assertEquals(null, page.prevKey)
        assertEquals(null, page.nextKey) // since items.size (2) < limit (50)

        // Verify getTotalMediaCount is NEVER called on the fast startup path!
        coVerify(exactly = 0) { mockRepository.getTotalMediaCount(any(), any(), any()) }
        coVerify(exactly = 1) { mockRepository.loadMediaPaged(50, 0, any(), any(), any(), any()) }
    }
}
