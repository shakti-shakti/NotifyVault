package com.example.data

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.util.Locale

enum class LinkSource {
    EXTRA_LINK_URI,
    EXTRA_SCAN,
    CONTENT_INTENT,
    ACTION_INTENT,
    LAUNCH_INTENT
}

enum class LinkConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class ExtractedLink(
    val uri: String,
    val source: LinkSource,
    val confidence: LinkConfidence
)

/**
 * Extracts a user-facing destination without mistaking notification assets and
 * analytics beacons for content links.
 */
object DeepLinkExtractor {
    private const val EXTRA_LINK_URI_KEY = "android.linkUri"

    private val appSchemes = setOf(
        "whatsapp", "tg", "instagram", "fb", "twitter", "x", "linkedin",
        "slack", "discord", "spotify", "youtube", "gmail", "googlegmail",
        "upi", "paytmmp", "phonepe", "gpay", "amazon", "flipkart",
        "swiggy", "zomato"
    )
    private val blockedHosts = listOf(
        "google-analytics", "doubleclick", "facebook.com/tr", "adjust.com",
        "branch.io"
    )
    private val blockedExtensions = listOf(
        ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".ico", ".woff",
        ".woff2", ".ttf", ".css", ".js", ".map"
    )

    fun extract(
        notification: Notification,
        packageName: String,
        context: Context
    ): ExtractedLink? {
        val extras = notification.extras ?: Bundle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val explicit = extras.getString(EXTRA_LINK_URI_KEY)
                ?: extras.getCharSequence(EXTRA_LINK_URI_KEY)?.toString()
            if (!explicit.isNullOrBlank()) {
                return ExtractedLink(explicit, LinkSource.EXTRA_LINK_URI, LinkConfidence.HIGH)
            }
        }

        val scanned = scanBundle(extras, packageName)
        if (scanned != null) {
            return ExtractedLink(scanned, LinkSource.EXTRA_SCAN, LinkConfidence.MEDIUM)
        }

        extractIntentData(notification.contentIntent)?.let {
            if (isValidDeepLink(it, packageName)) {
                return ExtractedLink(it, LinkSource.CONTENT_INTENT, LinkConfidence.MEDIUM)
            }
        }

        notification.actions.orEmpty().forEach { action ->
            extractIntentData(action.actionIntent)?.let {
                if (isValidDeepLink(it, packageName)) {
                    return ExtractedLink(it, LinkSource.ACTION_INTENT, LinkConfidence.MEDIUM)
                }
            }
        }

        try {
            context.packageManager.getLaunchIntentForPackage(packageName)?.dataString?.let {
                if (isValidDeepLink(it, packageName)) {
                    return ExtractedLink(it, LinkSource.LAUNCH_INTENT, LinkConfidence.LOW)
                }
            }
        } catch (_: Exception) {
            // Package visibility and uninstalled packages are expected cases.
        }
        return null
    }

    private fun scanBundle(bundle: Bundle, packageName: String): String? {
        return try {
            bundle.keySet().asSequence()
                .mapNotNull { key -> findCandidate(bundle.get(key), packageName) }
                .firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun findCandidate(value: Any?, packageName: String): String? {
        when (value) {
            is String -> if (isValidDeepLink(value, packageName)) return value
            is CharSequence -> if (isValidDeepLink(value.toString(), packageName)) return value.toString()
            is Uri -> if (isValidDeepLink(value.toString(), packageName)) return value.toString()
            is Bundle -> return scanBundle(value, packageName)
            is Parcelable -> {
                if (value is Uri && isValidDeepLink(value.toString(), packageName)) {
                    return value.toString()
                }
            }
            is Array<*> -> value.forEach { findCandidate(it, packageName)?.let { candidate -> return candidate } }
            is Iterable<*> -> value.forEach { findCandidate(it, packageName)?.let { candidate -> return candidate } }
        }
        return null
    }

    private fun extractIntentData(pendingIntent: android.app.PendingIntent?): String? {
        if (pendingIntent == null) return null
        return try {
            // PendingIntent#getIntent is hidden on modern Android. This is a
            // best-effort compatibility path; the live PendingIntent remains
            // available to ReplayEngine even when this inspection is blocked.
            val method = pendingIntent.javaClass.getDeclaredMethod("getIntent")
            method.isAccessible = true
            (method.invoke(pendingIntent) as? Intent)?.dataString
        } catch (_: Exception) {
            null
        }
    }

    internal fun isValidDeepLink(raw: String, packageName: String): Boolean {
        val value = raw.trim().trimEnd('.', ',', ';', ')', ']', '}')
        if (value.isBlank() || value.length > 4096) return false
        val lower = value.lowercase(Locale.ROOT)
        if (lower.startsWith("intent://")) {
            return lower.contains("#intent") &&
                lower.contains("package=${packageName.lowercase(Locale.ROOT)}")
        }
        if (lower.startsWith("android-app://")) return true

        val uri = try {
            Uri.parse(value)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme == "http" || scheme == "https") {
            val hostAndPath = lower.substringAfter("://")
            if (blockedHosts.any { hostAndPath.contains(it) }) return false
            if (blockedExtensions.any { hostAndPath.substringBefore('?').endsWith(it) }) return false
            if (hostAndPath.contains("favicon")) return false
            // Notification links are often short paths such as /verify or
            // /open. Rejecting those made valid app/browser destinations
            // impossible to replay from the vault.
            return uri.host?.isNotBlank() == true
        }
        return scheme in appSchemes ||
                scheme == packageName.lowercase(Locale.ROOT) ||
                scheme in setOf("mailto", "tel", "sms", "geo")
    }
}