package com.hrshd1eux.imava.core.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class FileShredderUtilTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testShredFile_destroysAndDeletesFile() = runBlocking {
        val testFile = tempFolder.newFile("sample_photo.jpg")
        FileOutputStream(testFile).use { fos ->
            // Write JPEG magic header + dummy pixel payload
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            val dummyData = ByteArray(128 * 1024) { (it % 256).toByte() }
            fos.write(dummyData)
        }
        assertTrue("Test file should exist before shredding", testFile.exists())
        assertTrue("Test file should have length > 0", testFile.length() > 0)

        val shredded = FileShredderUtil.shredFile(testFile)
        assertTrue("shredFile should return true", shredded)
        assertFalse("Test file should no longer exist after shredding", testFile.exists())
    }

    @Test
    fun testShredFile_zeroByteFile_deletesSafely() = runBlocking {
        val emptyFile = tempFolder.newFile("empty.tmp")
        assertTrue(emptyFile.exists())
        val result = FileShredderUtil.shredFile(emptyFile)
        assertTrue(result)
        assertFalse(emptyFile.exists())
    }

    @Test
    fun testShredFile_nonExistentFile_returnsFalse() = runBlocking {
        val nonExistent = File(tempFolder.root, "does_not_exist.jpg")
        assertFalse(nonExistent.exists())
        val result = FileShredderUtil.shredFile(nonExistent)
        assertFalse(result)
    }
}
