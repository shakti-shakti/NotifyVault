package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local PendingIntent registry. PendingIntents intentionally never
 * leave memory: they are not stable or safe to persist in Room.
 */
class LiveNotificationRegistry private constructor() {
    data class ActionEntry(
        val title: CharSequence,
        val actionIntent: PendingIntent?
    )

    data class LiveEntry(
        val key: String,
        val packageName: String,
        val notificationId: Int,
        val tag: String?,
        val postedAt: Long,
        val contentIntent: PendingIntent?,
        val deleteIntent: PendingIntent?,
        val actionIntents: List<ActionEntry>,
        val fullNotification: Notification
    )

    private val entries = ConcurrentHashMap<String, LiveEntry>()
    private val aliases = ConcurrentHashMap<String, String>()
    private val _liveKeys = MutableStateFlow<Set<String>>(emptySet())
    val liveKeys: StateFlow<Set<String>> = _liveKeys.asStateFlow()

    @Synchronized
    fun put(entry: LiveEntry) {
        val canonical = key(entry.packageName, entry.notificationId, entry.tag)
        entries[canonical] = entry
        aliases[canonical] = canonical
        aliases[entry.key] = canonical
        evictIfNeeded()
        emitKeys()
    }

    fun get(key: String): LiveEntry? {
        val canonical = aliases[key] ?: key
        return entries[canonical]
    }

    @Synchronized
    fun remove(key: String) {
        val canonical = aliases.remove(key) ?: key
        val removed = entries.remove(canonical)
        if (removed != null) {
            aliases.entries.removeIf { it.value == canonical }
            emitKeys()
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        aliases.clear()
        emitKeys()
    }

    fun contains(key: String): Boolean = get(key) != null
    fun size(): Int = entries.size
    fun snapshot(): List<LiveEntry> = entries.values.sortedByDescending { it.postedAt }

    private fun evictIfNeeded() {
        while (entries.size > MAX_ENTRIES) {
            val oldest = entries.values.minByOrNull { it.postedAt } ?: break
            entries.remove(key(oldest.packageName, oldest.notificationId, oldest.tag))
            aliases.entries.removeIf { it.value == key(oldest.packageName, oldest.notificationId, oldest.tag) }
        }
    }

    private fun emitKeys() {
        _liveKeys.value = entries.keys.toSet()
    }

    companion object {
        private const val MAX_ENTRIES = 500
        @Volatile private var instance: LiveNotificationRegistry? = null

        fun getInstance(): LiveNotificationRegistry =
            instance ?: synchronized(this) {
                instance ?: LiveNotificationRegistry().also { instance = it }
            }

        fun key(packageName: String, notificationId: Int, tag: String?): String =
            "$packageName|$notificationId|${tag.orEmpty()}"

        fun fromStatusBarNotification(sbn: StatusBarNotification): LiveEntry {
            val notification = Notification(sbn.notification)
            try {
                notification.extras?.apply {
                    remove(Notification.EXTRA_LARGE_ICON)
                    remove(Notification.EXTRA_PICTURE)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        remove("android.largeIcon.big")
                    }
                }
            } catch (error: Exception) {
                Log.w("NotifyVault", "Unable to strip notification extras", error)
            }
            return LiveEntry(
                key = sbn.key,
                packageName = sbn.packageName,
                notificationId = sbn.id,
                tag = sbn.tag,
                postedAt = sbn.postTime,
                contentIntent = sbn.notification.contentIntent,
                deleteIntent = sbn.notification.deleteIntent,
                actionIntents = sbn.notification.actions.orEmpty().map {
                    ActionEntry(it.title ?: "", it.actionIntent)
                },
                fullNotification = notification
            )
        }
    }
}