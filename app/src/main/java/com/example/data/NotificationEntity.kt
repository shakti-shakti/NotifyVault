package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["captureTime"]),
        Index(value = ["packageName"]),
        Index(value = ["category"]),
        Index(value = ["isStarred"]),
        Index(value = ["isArchived"]),
        Index(value = ["hasOtp"]),
        Index(value = ["hasAmount"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationKey: String = "",
    val notificationId: Int = 0,
    val tag: String? = null,
    val packageName: String,
    val appName: String,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val summaryText: String? = null,
    val infoText: String? = null,
    val appIconPath: String? = null,
    val smallIconPath: String? = null,
    val largeIconPath: String? = null,
    val picturePath: String? = null,
    val postTime: Long = System.currentTimeMillis(),
    val captureTime: Long = System.currentTimeMillis(),
    val category: String = "SYSTEM",
    val channelId: String? = null,
    val channelName: String? = null,
    val importance: Int = 3,
    val priority: Int = 0,
    val groupKey: String? = null,
    val sortKey: String? = null,
    val isGroupSummary: Boolean = false,
    val isOngoing: Boolean = false,
    val isClearable: Boolean = true,
    val isAutoCancel: Boolean = false,
    val isOnlyAlertOnce: Boolean = false,
    val hasSound: Boolean = false,
    val hasVibrate: Boolean = false,
    val hasLights: Boolean = false,
    val actionsJson: String = "[]",
    val actionLabels: String = "",
    val messagingMessagesJson: String = "[]",
    val peopleListJson: String = "[]",
    val isGroupConversation: Boolean = false,
    val senderName: String? = null,
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val isRead: Boolean = false,
    val hasOtp: Boolean = false,
    val otpCode: String? = null,
    val hasAmount: Boolean = false,
    val amountString: String? = null,
    val hasLink: Boolean = false,
    val linkUrl: String? = null,
    val rawExtrasJson: String = "{}"
)
