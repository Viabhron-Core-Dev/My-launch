package com.aistudio.mylauncher.abcd

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_items")
data class WorkspaceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: Int, // 0=app, 1=shortcut, 2=folder, 3=widget
    val packageName: String,
    val activityName: String,
    val container: Int, // 0=desktop, 1=dock, 2=folder
    val containerId: Long, // -1 if not in a folder
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val pageIndex: Int,
    val title: String,
    val customIconPath: String,
    val appWidgetId: Int // -1 if not a widget
)
