package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewRepositoryTest {

    @Test
    fun testGetReleasesIsNotEmpty() {
        val releases = WhatsNewRepository.getReleases()
        assertFalse("Releases list should not be empty", releases.isEmpty())
    }

    @Test
    fun testLatestReleaseIsCurrent() {
        val releases = WhatsNewRepository.getReleases()
        val latest = releases.first()
        assertEquals(WhatsNewRepository.CURRENT_VERSION_NAME, latest.version)
        assertTrue("Latest release must be marked current", latest.isCurrent)
        assertFalse("Items for latest release must not be empty", latest.items.isEmpty())
        assertTrue("Items count should cover all major changes", latest.items.size >= 8)
    }

    @Test
    fun testAllReleasesHaveValidItems() {
        val releases = WhatsNewRepository.getReleases()
        for (release in releases) {
            assertFalse("Release version should not be blank", release.version.isBlank())
            assertFalse("Release items should not be empty", release.items.isEmpty())
            for (item in release.items) {
                assertTrue("Title Res ID must be valid", item.titleRes != 0)
                assertTrue("Desc Res ID must be valid", item.descRes != 0)
                assertNotNull("Category must not be null", item.category)
            }
        }
    }

    @Test
    fun testFindReleaseByVersion() {
        val releases = WhatsNewRepository.getReleases()
        val vLatest = releases.find { it.version == WhatsNewRepository.CURRENT_VERSION_NAME }
        assertNotNull("${WhatsNewRepository.CURRENT_VERSION_NAME} must exist", vLatest)
        assertEquals(WhatsNewRepository.CURRENT_VERSION_NAME, vLatest?.version)

        val v112 = releases.find { it.version == "1.1.112" }
        assertNotNull("1.1.112 must exist", v112)
        assertEquals("1.1.112", v112?.version)
    }
}
