package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewRepositoryTest {

    @Test
    fun testGetReleasesContainsCurrentAndLastTwoVersions() {
        val releases = WhatsNewRepository.getReleases()
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases.size)
        assertEquals(WhatsNewRepository.CURRENT_VERSION_NAME, releases[0].version)
        assertEquals("1.1.120", releases[1].version)
        assertEquals("1.1.119", releases[2].version)
    }

    @Test
    fun testLatestReleaseIsCurrent() {
        val releases = WhatsNewRepository.getReleases()
        val latest = releases.first()
        assertEquals(WhatsNewRepository.CURRENT_VERSION_NAME, latest.version)
        assertTrue("Latest release must be marked current", latest.isCurrent)
        assertFalse("Items for latest release must not be empty", latest.items.isEmpty())
        assertTrue("Items count should cover all major changes", latest.items.size >= 4)
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

        val v120 = releases.find { it.version == "1.1.120" }
        assertNotNull("1.1.120 must exist", v120)
        assertEquals("1.1.120", v120?.version)

        val v119 = releases.find { it.version == "1.1.119" }
        assertNotNull("1.1.119 must exist", v119)
        assertEquals("1.1.119", v119?.version)
    }
}
