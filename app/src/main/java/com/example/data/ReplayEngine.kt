package com.example.data

import android.app.ActivityNotFoundException
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.service.LiveNotificationRegistry

sealed class ReplayResult {
    data object ExactReplay : ReplayResult()
    data object ActionReplay : ReplayResult()
    data object DeepLinkReplay : ReplayResult()
    data object AppLaunch : ReplayResult()
    data object AppDetails : ReplayResult()
    data object Failed : ReplayResult()
}

class ReplayEngine internal constructor(
    private val context: Context,
    private val registry: LiveNotificationRegistry
) {
    fun replay(entity: NotificationEntity): ReplayResult {
        val entry = registry.get(keyFor(entity))
        if (entry?.contentIntent != null) {
            try {
                entry.contentIntent.send()
                return ReplayResult.ExactReplay
            } catch (_: PendingIntent.CanceledException) {
                registry.remove(keyFor(entity))
            } catch (_: Exception) {
                // Continue down the fallback chain.
            }
        }

        replayDeepLink(entity)?.let { return it }
        return launchAppOrDetails(entity.packageName)
    }

    fun replayAction(entity: NotificationEntity, actionTitle: String): ReplayResult {
        val entry = registry.get(keyFor(entity)) ?: return ReplayResult.Failed
        val action = entry.actionIntents.firstOrNull {
            it.title.toString().equals(actionTitle, ignoreCase = true)
        } ?: return ReplayResult.Failed
        return try {
            action.actionIntent?.send() ?: return ReplayResult.Failed
            ReplayResult.ActionReplay
        } catch (_: Exception) {
            ReplayResult.Failed
        }
    }

    fun isExactReplayReady(entity: NotificationEntity): Boolean =
        registry.get(keyFor(entity))?.contentIntent != null

    fun isActionLive(entity: NotificationEntity, title: String): Boolean =
        registry.get(keyFor(entity))?.actionIntents?.any {
            it.title.toString().equals(title, ignoreCase = true) && it.actionIntent != null
        } == true

    private fun replayDeepLink(entity: NotificationEntity): ReplayResult? {
        val uri = entity.deepLinkUri ?: return null
        return try {
            val scoped = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setPackage(entity.packageName)
            context.startActivity(scoped)
            ReplayResult.DeepLinkReplay
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                ReplayResult.DeepLinkReplay
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun launchAppOrDetails(packageName: String): ReplayResult {
        try {
            context.packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ReplayResult.AppLaunch
            }
        } catch (_: Exception) {
            // Try app details below.
        }

        return try {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ReplayResult.AppDetails
        } catch (_: Exception) {
            ReplayResult.Failed
        }
    }

    private fun keyFor(entity: NotificationEntity): String =
        LiveNotificationRegistry.key(entity.packageName, entity.notificationId, entity.tag)

    companion object {
        @Volatile private var instance: ReplayEngine? = null

        fun getInstance(context: Context): ReplayEngine =
            instance ?: synchronized(this) {
                instance ?: ReplayEngine(
                    context.applicationContext,
                    LiveNotificationRegistry.getInstance()
                ).also { instance = it }
            }
    }
}