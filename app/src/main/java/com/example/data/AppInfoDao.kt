package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM app_info_cache WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppInfo(packageName: String): AppInfoEntity?

    @Query("SELECT * FROM app_info_cache")
    suspend fun getAllAppInfo(): List<AppInfoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appInfo: AppInfoEntity)

    @Query("DELETE FROM app_info_cache WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
