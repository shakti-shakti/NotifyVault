package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info_cache")
data class AppInfoEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val iconPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
