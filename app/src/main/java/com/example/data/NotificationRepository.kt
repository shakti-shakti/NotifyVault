package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

class NotificationRepository(private val dao: NotificationDao) {

    val activeNotifications: Flow<List<NotificationEntity>> = dao.getAllActive()
    val archivedNotifications: Flow<List<NotificationEntity>> = dao.getAllArchived()
    val starredNotifications: Flow<List<NotificationEntity>> = dao.getStarred()
    val otpNotifications: Flow<List<NotificationEntity>> = dao.getOtps()
    val paymentNotifications: Flow<List<NotificationEntity>> = dao.getPayments()
    val deliveryNotifications: Flow<List<NotificationEntity>> = dao.getDeliveries()
    val messageNotifications: Flow<List<NotificationEntity>> = dao.getMessages()

    val totalCount: Flow<Int> = dao.getTotalCount()
    val otpCount: Flow<Int> = dao.getOtpCount()
    val paymentCount: Flow<Int> = dao.getPaymentCount()
    val starredCount: Flow<Int> = dao.getStarredCount()

    fun getTodayCount(startOfDay: Long): Flow<Int> = dao.getTodayCount(startOfDay)

    fun search(query: String): Flow<List<NotificationEntity>> = dao.search(query)

    fun getById(id: Long): Flow<NotificationEntity?> = dao.getById(id)

    fun getHistory(id: Long): Flow<List<NotificationHistoryEntity>> = dao.getHistory(id)

    suspend fun insert(notification: NotificationEntity): Long = dao.insert(notification)

    suspend fun setStarred(id: Long, isStarred: Boolean) = dao.setStarred(id, isStarred)

    suspend fun setArchived(id: Long, isArchived: Boolean) = dao.setArchived(id, isArchived)

    suspend fun markAsRead(id: Long) = dao.markAsRead(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun starByIds(ids: List<Long>) = dao.starByIds(ids)

    suspend fun archiveByIds(ids: List<Long>) = dao.archiveByIds(ids)

    suspend fun clearAll() = dao.clearAll()

    companion object {
        // Smart tag parsing logic for incoming notifications
        private val OTP_REGEX = Pattern.compile("(?<!\\d)(\\d{4}|\\d{6}|\\d{8})(?!\\d)")
        private val OTP_KEYWORD_REGEX = Pattern.compile(
            "\\b(otp|one[- ]?time password|verification code|verify code|security code|auth(?:entication)? code|login code|sign[- ]in code|confirmation code|passcode|pin|code)\\b",
            Pattern.CASE_INSENSITIVE
        )
        private val AMOUNT_REGEX = Pattern.compile("(?:₹|Rs\\.?|\\$|€|£|INR)\\s?([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        private val LINK_REGEX = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)

        fun parseNotification(
            packageName: String,
            appName: String,
            title: String?,
            text: String?,
            bigText: String?,
            subText: String?,
            category: String = "SYSTEM",
            channelId: String? = null,
            channelName: String? = null,
            key: String = "",
            extrasJson: String = "{}"
        ): NotificationEntity {
            val fullContent = "${title.orEmpty()} ${text.orEmpty()} ${bigText.orEmpty()}"

            // Check OTP
            var hasOtp = false
            var otpCode: String? = null
            if (OTP_KEYWORD_REGEX.matcher(fullContent).find()) {
                val matches = OTP_REGEX.matcher(fullContent)
                val candidates = mutableListOf<String>()
                while (matches.find()) candidates += matches.group(1)
                val detected = candidates.firstOrNull { it.length == 6 }
                    ?: candidates.firstOrNull { it.length == 8 }
                    ?: candidates.firstOrNull { it.length == 4 }
                if (detected != null) {
                    hasOtp = true
                    otpCode = detected
                }
            }

            // Check Amount
            var hasAmount = false
            var amountString: String? = null
            val amountMatcher = AMOUNT_REGEX.matcher(fullContent)
            if (amountMatcher.find()) {
                hasAmount = true
                amountString = amountMatcher.group(0)
            }

            // Check Link
            var hasLink = false
            var linkUrl: String? = null
            val linkMatcher = LINK_REGEX.matcher(fullContent)
            if (linkMatcher.find()) {
                hasLink = true
                linkUrl = linkMatcher.group(0)
            }

            val determinedCategory = when {
                hasOtp -> "OTP"
                hasAmount -> "PAYMENT"
                packageName.contains("whatsapp") || packageName.contains("telegram") || packageName.contains("signal") -> "SOCIAL"
                packageName.contains("amazon") || packageName.contains("flipkart") || packageName.contains("fedex") || packageName.contains("zomato") || packageName.contains("swiggy") -> "DELIVERY"
                packageName.contains("bank") || packageName.contains("pay") || packageName.contains("credit") -> "PAYMENT"
                else -> category
            }

            return NotificationEntity(
                notificationKey = key,
                packageName = packageName,
                appName = appName,
                title = title,
                text = text,
                bigText = bigText,
                subText = subText,
                category = determinedCategory,
                channelId = channelId,
                channelName = channelName,
                hasOtp = hasOtp,
                otpCode = otpCode,
                hasAmount = hasAmount,
                amountString = amountString,
                hasLink = hasLink,
                linkUrl = linkUrl,
                rawExtrasJson = extrasJson,
                postTime = System.currentTimeMillis(),
                captureTime = System.currentTimeMillis()
            )
        }
    }
}
