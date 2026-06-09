package com.aistudio.mylauncher.abcd

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val packageName: String,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val customIconPath: String,
    val customLabel: String
)
