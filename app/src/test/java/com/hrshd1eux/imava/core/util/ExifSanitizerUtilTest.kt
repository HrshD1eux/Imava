package com.hrshd1eux.imava.core.util

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifSanitizerUtilTest {

    @Test
    fun testSensitiveTags_containsAllEssentialPrivacyTags() {
        val tags = ExifSanitizerUtil.SENSITIVE_TAGS

        // GPS checks
        assertTrue("Must strip GPS latitude", tags.contains(ExifInterface.TAG_GPS_LATITUDE))
        assertTrue("Must strip GPS longitude", tags.contains(ExifInterface.TAG_GPS_LONGITUDE))
        assertTrue("Must strip GPS altitude", tags.contains(ExifInterface.TAG_GPS_ALTITUDE))
        assertTrue("Must strip GPS datestamp", tags.contains(ExifInterface.TAG_GPS_DATESTAMP))
        assertTrue("Must strip GPS timestamp", tags.contains(ExifInterface.TAG_GPS_TIMESTAMP))
        assertTrue("Must strip GPS speed", tags.contains(ExifInterface.TAG_GPS_SPEED))

        // DateTime checks
        assertTrue("Must strip Datetime", tags.contains(ExifInterface.TAG_DATETIME))
        assertTrue("Must strip Datetime Original", tags.contains(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertTrue("Must strip Datetime Digitized", tags.contains(ExifInterface.TAG_DATETIME_DIGITIZED))
        assertTrue("Must strip Subsec Time", tags.contains(ExifInterface.TAG_SUBSEC_TIME))
        assertTrue("Must strip Subsec Time Original", tags.contains(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL))
        assertTrue("Must strip Offset Time", tags.contains(ExifInterface.TAG_OFFSET_TIME))

        // Device & Serial checks
        assertTrue("Must strip Device Make", tags.contains(ExifInterface.TAG_MAKE))
        assertTrue("Must strip Device Model", tags.contains(ExifInterface.TAG_MODEL))
        assertTrue("Must strip Camera Owner", tags.contains(ExifInterface.TAG_CAMERA_OWNER_NAME))
        assertTrue("Must strip Body Serial Number", tags.contains(ExifInterface.TAG_BODY_SERIAL_NUMBER))
        assertTrue("Must strip Lens Model", tags.contains(ExifInterface.TAG_LENS_MODEL))
        assertTrue("Must strip Lens Serial Number", tags.contains(ExifInterface.TAG_LENS_SERIAL_NUMBER))
        assertTrue("Must strip Device Settings", tags.contains(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION))

        // Blobs & User comments
        assertTrue("Must strip User Comment", tags.contains(ExifInterface.TAG_USER_COMMENT))
        assertTrue("Must strip Image Description", tags.contains(ExifInterface.TAG_IMAGE_DESCRIPTION))
        assertTrue("Must strip Maker Note", tags.contains(ExifInterface.TAG_MAKER_NOTE))
        assertTrue("Must strip XMP", tags.contains(ExifInterface.TAG_XMP))
    }
}
