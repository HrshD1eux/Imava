package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewRepositoryTest {

    @Test
    fun testGetReleasesContainsCurrentAndLastTwoVersions() {
        val releases130 = WhatsNewRepository.getReleases("1.1.130")
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases130.size)
        assertEquals("1.1.130", releases130[0].version)
        assertEquals("1.1.129", releases130[1].version)
        assertEquals("1.1.128", releases130[2].version)

        val releases129 = WhatsNewRepository.getReleases("1.1.129")
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases129.size)
        assertEquals("1.1.129", releases129[0].version)
        assertEquals("1.1.128", releases129[1].version)
        assertEquals("1.1.127", releases129[2].version)

        val releases128 = WhatsNewRepository.getReleases("1.1.128")
        assertEquals("Should contain exactly 3 releases (current and last 2)", 3, releases128.size)
        assertEquals("1.1.128", releases128[0].version)
        assertEquals("1.1.127", releases128[1].version)
        assertEquals("1.1.126", releases128[2].version)

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
        assertEquals("1.1.128", WhatsNewRepository.sanitizeVersion("1.1.128-DEBUG"))
        assertEquals("1.1.128", WhatsNewRepository.sanitizeVersion("1.1.128+45"))
        assertEquals("1.1.128", WhatsNewRepository.sanitizeVersion(" 1.1.128 "))
        assertEquals("1.1.123", WhatsNewRepository.sanitizeVersion("1.1.123-DEBUG"))
    }

    @Test
    fun testGetPreviousVersionMath() {
        assertEquals("1.1.127", WhatsNewRepository.getPreviousVersion("1.1.128", 1))
        assertEquals("1.1.126", WhatsNewRepository.getPreviousVersion("1.1.128", 2))
        assertEquals("1.1.125", WhatsNewRepository.getPreviousVersion("1.1.128", 3))
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
    fun testCurrentVersionHasDedicatedAndUniqueReleaseNotes() {
        val currentVersion = WhatsNewRepository.CURRENT_VERSION_NAME
        val currentItems = WhatsNewRepository.getItemsForVersion(currentVersion)
        assertFalse("Current version release notes must not be empty", currentItems.isEmpty())
        assertTrue("Current version must have at least 4 release highlights", currentItems.size >= 4)

        val prevVersion = WhatsNewRepository.getPreviousVersion(currentVersion, 1)
        val prevItems = WhatsNewRepository.getItemsForVersion(prevVersion)
        assertFalse("Previous version release notes must not be empty", prevItems.isEmpty())

        val currentTitleRes = currentItems.map { it.titleRes }.toSet()
        val prevTitleRes = prevItems.map { it.titleRes }.toSet()
        assertTrue(
            "Current version $currentVersion must have distinct release notes from $prevVersion",
            currentTitleRes != prevTitleRes
        )
    }

    @Test
    fun testAllReleasesHaveValidItems() {
        val releases = WhatsNewRepository.getReleases(WhatsNewRepository.CURRENT_VERSION_NAME)
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
        val releases = WhatsNewRepository.getReleases("1.1.130")
        val vLatest = releases.find { it.version == "1.1.130" }
        assertNotNull("1.1.130 must exist", vLatest)
        assertEquals("1.1.130", vLatest?.version)

        val v129 = releases.find { it.version == "1.1.129" }
        assertNotNull("1.1.129 must exist", v129)
        assertEquals("1.1.129", v129?.version)

        val v128 = releases.find { it.version == "1.1.128" }
        assertNotNull("1.1.128 must exist", v128)
        assertEquals("1.1.128", v128?.version)
    }
}
