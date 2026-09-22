package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE isArchived = 0 ORDER BY captureTime DESC")
    fun getAllActive(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isArchived = 1 ORDER BY captureTime DESC")
    fun getAllArchived(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isStarred = 1 ORDER BY captureTime DESC")
    fun getStarred(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE hasOtp = 1 AND isArchived = 0 ORDER BY captureTime DESC")
    fun getOtps(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE hasAmount = 1 AND isArchived = 0 ORDER BY captureTime DESC")
    fun getPayments(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE (category = 'DELIVERY' OR category = 'COMMUNICATION' OR text LIKE '%deliver%' OR text LIKE '%order%' OR title LIKE '%order%') AND isArchived = 0 ORDER BY captureTime DESC")
    fun getDeliveries(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE (category = 'SOCIAL' OR category = 'MESSAGES' OR category = 'CHAT') AND isArchived = 0 ORDER BY captureTime DESC")
    fun getMessages(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun getById(id: Long): Flow<NotificationEntity?>

    @Query("""
        SELECT * FROM notifications 
        WHERE isArchived = 0 
        AND (
            title LIKE '%' || :query || '%' 
            OR text LIKE '%' || :query || '%' 
            OR appName LIKE '%' || :query || '%' 
            OR packageName LIKE '%' || :query || '%'
            OR otpCode LIKE '%' || :query || '%'
            OR senderName LIKE '%' || :query || '%'
        )
        ORDER BY captureTime DESC
    """)
    fun search(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE captureTime >= :sinceTimestamp")
    fun getTodayCount(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE hasOtp = 1")
    fun getOtpCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE hasAmount = 1")
    fun getPaymentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isStarred = 1")
    fun getStarredCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE notifications SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE notifications SET isStarred = 1 WHERE id IN (:ids)")
    suspend fun starByIds(ids: List<Long>)

    @Query("UPDATE notifications SET isArchived = 1 WHERE id IN (:ids)")
    suspend fun archiveByIds(ids: List<Long>)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
