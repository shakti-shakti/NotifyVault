package com.example.service

import com.example.data.NotificationDao
import com.example.data.NotificationEntity

sealed class DedupDecision {
    data object InsertNew : DedupDecision()
    data object UpdateExisting : DedupDecision()
    data object SkipDuplicate : DedupDecision()
}

class NotificationDeduper(private val dao: NotificationDao) {
    suspend fun decide(fp: Fingerprint): DedupDecision {
        val existing = dao.getLatestByFingerprintKey(fp.key) ?: return DedupDecision.InsertNew
        if (fp.packageName == "android" && existing.contentHash != fp.contentHash) {
            return DedupDecision.InsertNew
        }
        if (existing.isOngoing || existing.isGroupSummary || fp.isOngoing || fp.isGroupSummary) {
            dao.touchLastSeen(existing.id, fp.updateTime)
            return DedupDecision.SkipDuplicate
        }
        if (existing.contentHash == fp.contentHash) {
            dao.touchLastSeen(existing.id, fp.updateTime)
            return DedupDecision.SkipDuplicate
        }
        val gap = fp.updateTime - existing.lastSeenAt
        val isTypingConversation =
            fp.packageName.contains("whatsapp", true) ||
                fp.packageName.contains("telegram", true) ||
                fp.packageName.contains("signal", true)
        if (isTypingConversation && gap <= TWO_MINUTES) {
            dao.touchLastSeen(existing.id, fp.updateTime)
            return DedupDecision.SkipDuplicate
        }
        return DedupDecision.UpdateExisting
    }

    suspend fun existing(fp: Fingerprint): NotificationEntity? =
        dao.getLatestByFingerprintKey(fp.key)

    companion object {
        private const val TWO_MINUTES = 120_000L
    }
}