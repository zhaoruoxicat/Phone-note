package com.lyx.phone.note.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun normalizesChineseMobileFormats() {
        assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("13800138000").normalizedNumber)
        assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("+86 138 0013 8000").normalizedNumber)
        assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("008613800138000").normalizedNumber)
        assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("086-13800138000").normalizedNumber)
    }

    @Test
    fun keepsServiceAndLandlineNumbersComplete() {
        assertEquals("4008001234", PhoneNumberNormalizer.normalize("400-800-1234").normalizedNumber)
        assertEquals("95588", PhoneNumberNormalizer.normalize("95588").normalizedNumber)
        assertEquals("01088886666", PhoneNumberNormalizer.normalize("010-8888-6666").normalizedNumber)
    }

    @Test
    fun exposesLast11DigitsOnlyForMobile() {
        val mobile = PhoneNumberNormalizer.normalize("+86 138 0013 8000")
        assertEquals("13800138000", mobile.last11Digits)
        assertTrue(mobile.isLikelyChineseMobile)

        val landline = PhoneNumberNormalizer.normalize("010-8888-6666")
        assertEquals(null, landline.last11Digits)
        assertFalse(landline.isLikelyChineseMobile)
    }

    @Test
    fun masksNumbersForLogs() {
        assertEquals("+86 138****8000", PhoneNumberNormalizer.mask("+8613800138000"))
        assertEquals("400****234", PhoneNumberNormalizer.mask("4008001234"))
    }
}
