package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewRepositoryTest {

    @Test
    fun testGetReleasesContainsCurrentAndLastTwoVersions() {
        val releases123 = WhatsNewRepository.getReleases("1.1.123")
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases123.size)
        assertEquals("1.1.123", releases123[0].version)
        assertEquals("1.1.122", releases123[1].version)
        assertEquals("1.1.121", releases123[2].version)

        val releases122 = WhatsNewRepository.getReleases("1.1.122")
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases122.size)
        assertEquals("1.1.122", releases122[0].version)
        assertEquals("1.1.121", releases122[1].version)
        assertEquals("1.1.120", releases122[2].version)
    }

    @Test
    fun testSanitizeVersion() {
        assertEquals("1.1.123", WhatsNewRepository.sanitizeVersion("1.1.123-DEBUG"))
        assertEquals("1.1.123", WhatsNewRepository.sanitizeVersion("1.1.123+45"))
        assertEquals("1.1.123", WhatsNewRepository.sanitizeVersion(" 1.1.123 "))
    }

    @Test
    fun testGetPreviousVersionMath() {
        assertEquals("1.1.122", WhatsNewRepository.getPreviousVersion("1.1.123", 1))
        assertEquals("1.1.121", WhatsNewRepository.getPreviousVersion("1.1.123", 2))
        assertEquals("1.1.120", WhatsNewRepository.getPreviousVersion("1.1.123", 3))
        assertEquals("1.1.120", WhatsNewRepository.getPreviousVersion("1.1.121", 1))
        assertEquals("1.1.119", WhatsNewRepository.getPreviousVersion("1.1.121", 2))
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
        val releases = WhatsNewRepository.getReleases("1.1.123")
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
        val releases = WhatsNewRepository.getReleases("1.1.123")
        val vLatest = releases.find { it.version == "1.1.123" }
        assertNotNull("1.1.123 must exist", vLatest)
        assertEquals("1.1.123", vLatest?.version)

        val v122 = releases.find { it.version == "1.1.122" }
        assertNotNull("1.1.122 must exist", v122)
        assertEquals("1.1.122", v122?.version)

        val v121 = releases.find { it.version == "1.1.121" }
        assertNotNull("1.1.121 must exist", v121)
        assertEquals("1.1.121", v121?.version)
    }
}
