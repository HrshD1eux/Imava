package com.hrshd1eux.imava.ui.viewer

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.hrshd1eux.imava.core.util.VaultCrypto
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Random
import javax.crypto.KeyGenerator

class EncryptedVaultDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        VaultCrypto.testSecretKey = keyGen.generateKey()
    }

    @After
    fun tearDown() {
        VaultCrypto.testSecretKey = null
    }

    @Test
    fun testOpenAndRead_allBytesMatchOriginal() {
        val original = ByteArray(128 * 1024) // 128 KB
        Random(42).nextBytes(original)

        val encryptedFile = tempFolder.newFile("encrypted.bin")
        FileOutputStream(encryptedFile).use { fos ->
            VaultCrypto.encrypt(ByteArrayInputStream(original), fos)
        }

        val dataSource = EncryptedVaultDataSource(encryptedFile)
        val mockUri = mockk<Uri>(relaxed = true)
        val dataSpec = DataSpec.Builder().setUri(mockUri).build()

        val remaining = dataSource.open(dataSpec)
        assertEquals(original.size.toLong(), remaining)

        val readBuffer = ByteArray(original.size)
        var totalRead = 0
        while (totalRead < original.size) {
            val count = dataSource.read(readBuffer, totalRead, original.size - totalRead)
            if (count == -1) break
            totalRead += count
        }

        assertEquals(original.size, totalRead)
        assertArrayEquals(original, readBuffer)
        dataSource.close()
    }

    @Test
    fun testOpenAndSeek_fastScrubbingMatchesExpectedOffset() {
        val original = ByteArray(200 * 1024) // 200 KB
        Random(123).nextBytes(original)

        val encryptedFile = tempFolder.newFile("seek_test.bin")
        FileOutputStream(encryptedFile).use { fos ->
            VaultCrypto.encrypt(ByteArrayInputStream(original), fos)
        }

        val seekOffset = 130 * 1024L // 130 KB offset (> 64KB skip buffer to exercise loop)
        val length = (original.size - seekOffset)
        val dataSource = EncryptedVaultDataSource(encryptedFile)
        val mockUri = mockk<Uri>(relaxed = true)
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .setPosition(seekOffset)
            .setLength(length)
            .build()

        val remaining = dataSource.open(dataSpec)
        assertEquals(length, remaining)

        val expectedBytes = original.copyOfRange(seekOffset.toInt(), original.size)
        val readBuffer = ByteArray(expectedBytes.size)
        var totalRead = 0
        while (totalRead < expectedBytes.size) {
            val count = dataSource.read(readBuffer, totalRead, expectedBytes.size - totalRead)
            if (count == -1) break
            totalRead += count
        }

        assertEquals(expectedBytes.size, totalRead)
        assertArrayEquals(expectedBytes, readBuffer)
        dataSource.close()
    }

    @Test(expected = IOException::class)
    fun testOpen_missingIvHeaderThrowsIOException() {
        val corruptFile = tempFolder.newFile("corrupt.bin")
        corruptFile.writeBytes(byteArrayOf(1, 2, 3)) // Only 3 bytes (< 12 IV)

        val dataSource = EncryptedVaultDataSource(corruptFile)
        val mockUri = mockk<Uri>(relaxed = true)
        dataSource.open(DataSpec.Builder().setUri(mockUri).build())
    }
}
