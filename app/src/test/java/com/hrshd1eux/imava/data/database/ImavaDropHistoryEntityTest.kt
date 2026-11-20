package com.hrshd1eux.imava.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImavaDropHistoryEntityTest {

    @Test
    fun testEntityDefaultValues() {
        val entity = ImavaDropHistoryEntity(
            fileName = "IMG_2026.jpg",
            fileSizeBytes = 1048576L,
            direction = ImavaDropHistoryEntity.DIRECTION_SENT,
            clientIp = "192.168.1.105",
            timestamp = 1700000000000L
        )

        assertEquals("IMG_2026.jpg", entity.fileName)
        assertEquals(1048576L, entity.fileSizeBytes)
        assertEquals(ImavaDropHistoryEntity.DIRECTION_SENT, entity.direction)
        assertEquals("192.168.1.105", entity.clientIp)
        assertEquals(ImavaDropHistoryEntity.STATUS_SUCCESS, entity.status)
        assertEquals(null, entity.mediaUri)
    }

    @Test
    fun testEntityConstants() {
        assertEquals("SENT", ImavaDropHistoryEntity.DIRECTION_SENT)
        assertEquals("RECEIVED", ImavaDropHistoryEntity.DIRECTION_RECEIVED)
        assertEquals("SUCCESS", ImavaDropHistoryEntity.STATUS_SUCCESS)
        assertEquals("FAILED", ImavaDropHistoryEntity.STATUS_FAILED)
    }
}
