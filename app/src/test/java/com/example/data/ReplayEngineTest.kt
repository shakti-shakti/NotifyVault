package com.example.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.LiveNotificationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class ReplayEngineTest {
    private lateinit var context: android.content.Context
    private lateinit var registry: LiveNotificationRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registry = LiveNotificationRegistry.getInstance()
        registry.clear()
    }

    @Test
    fun emptyRegistryFallsThroughToAppLaunch() {
        val result = ReplayEngine(context, registry).replay(entity())

        assertTrue(result == ReplayResult.AppLaunch || result == ReplayResult.AppDetails)
    }

    @Test
    fun canceledPendingIntentFallsThroughWithoutThrowing() {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            81,
            Intent("com.example.CANCELED_REPLAY"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        registry.put(
            LiveNotificationRegistry.LiveEntry(
                key = "sbn-key",
                packageName = context.packageName,
                notificationId = 81,
                tag = null,
                postedAt = 1L,
                contentIntent = pendingIntent,
                deleteIntent = null,
                actionIntents = emptyList(),
                fullNotification = Notification()
            )
        )
        pendingIntent.cancel()

        val result = ReplayEngine(context, registry).replay(entity())

        assertTrue(
            result == ReplayResult.AppLaunch ||
                result == ReplayResult.AppDetails ||
                result == ReplayResult.Failed
        )
    }

    private fun entity() = NotificationEntity(
        packageName = context.packageName,
        appName = "Test",
        notificationId = 81
    )
}