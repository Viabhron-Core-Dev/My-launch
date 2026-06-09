package com.aistudio.mylauncher.abcd

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_info")
data class FolderInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cellX: Int,
    val cellY: Int,
    val pageIndex: Int,
    val colorHex: String = "#FF5722",
    val customIconPath: String
)
