package com.example.data

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkExtractorTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun extractsKnownAppSchemeFromExtras() {
        val notification = Notification().apply {
            extras.putString("destination", "whatsapp://send?text=hello")
        }

        val result = DeepLinkExtractor.extract(notification, "com.example.sender", context)

        assertEquals("whatsapp://send?text=hello", result?.uri)
        assertEquals(LinkSource.EXTRA_SCAN, result?.source)
        assertEquals(LinkConfidence.MEDIUM, result?.confidence)
    }

    @Test
    fun ignoresImageUrlNoise() {
        val notification = Notification().apply {
            extras.putString("image", "https://cdn.example.com/banner.png")
        }

        assertNull(DeepLinkExtractor.extract(notification, "com.example.sender", context))
    }
}