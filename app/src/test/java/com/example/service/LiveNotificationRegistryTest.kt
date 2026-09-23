package com.example.service

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNotificationRegistryTest {
    @Test
    fun evictsOldestEntryAfterFiveHundredEntries() {
        val registry = LiveNotificationRegistry.getInstance()
        registry.clear()

        repeat(501) { index ->
            registry.put(
                LiveNotificationRegistry.LiveEntry(
                    key = "sbn-$index",
                    packageName = "com.example.app$index",
                    notificationId = index,
                    tag = null,
                    postedAt = index.toLong(),
                    contentIntent = null,
                    deleteIntent = null,
                    actionIntents = emptyList(),
                    fullNotification = Notification()
                )
            )
        }

        assertEquals(500, registry.size())
        assertFalse(registry.contains("com.example.app0|0|"))
        assertTrue(registry.contains("com.example.app500|500|"))
        registry.clear()
    }
}