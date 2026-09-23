package com.example.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.security.MessageDigest

data class Fingerprint(
    val key: String,
    val contentHash: String,
    val postedAt: Long,
    val updateTime: Long,
    val packageName: String = "",
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false
)

object NotificationFingerprint {
    fun of(sbn: StatusBarNotification): Fingerprint {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val summaryText = extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty()
        val infoText = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
        val actionLabels = notification.actions
            ?.mapNotNull { it.title?.toString() }
            ?.joinToString("\u001f")
            .orEmpty()
        val dataUri = notification.contentIntent?.let { "content-intent" }.orEmpty()
        val progress = if (
            extras?.containsKey(Notification.EXTRA_PROGRESS) == true ||
            extras?.containsKey(Notification.EXTRA_PROGRESS_MAX) == true ||
            extras?.containsKey(Notification.EXTRA_PROGRESS_INDETERMINATE) == true
        ) {
            "${notification.extras?.getInt(Notification.EXTRA_PROGRESS, -1)}:" +
                "${notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX, -1)}:" +
                "${notification.extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)}"
        } else {
            "none"
        }
        val ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val groupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val canonical = listOf(
            title, text, bigText, subText, summaryText, infoText, actionLabels,
            dataUri, ongoing.toString(), groupSummary.toString(), progress
        ).joinToString("\u001e") { it.trim() }
        val key = "${sbn.packageName}|${sbn.id}|${sbn.tag ?: ""}|${sbn.user}"
        return Fingerprint(
            key = key,
            contentHash = sha256(canonical),
            postedAt = sbn.postTime,
            updateTime = System.currentTimeMillis(),
            packageName = sbn.packageName,
            isOngoing = ongoing,
            isGroupSummary = groupSummary
        )
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}