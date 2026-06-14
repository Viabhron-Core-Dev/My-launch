package com.aistudio.mylauncher.abcd

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkspaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkspaceItem)

    @Update
    suspend fun update(item: WorkspaceItem)

    @Delete
    suspend fun delete(item: WorkspaceItem)

    @Query("SELECT * FROM workspace_items")
    suspend fun getAll(): List<WorkspaceItem>

    @Query("SELECT * FROM workspace_items WHERE pageIndex = :pageIndex AND container = :container")
    suspend fun getAllForPage(pageIndex: Int, container: Int): List<WorkspaceItem>

    @Query("SELECT * FROM workspace_items WHERE container = :container")
    suspend fun getAllForContainer(container: Int): List<WorkspaceItem>

    @Query("DELETE FROM workspace_items WHERE container = 0")
    suspend fun clearHomeScreen()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderInfo): Long

    @Update
    suspend fun updateFolder(folder: FolderInfo)

    @Delete
    suspend fun deleteFolder(folder: FolderInfo)

    @Query("SELECT * FROM folder_info")
    suspend fun getAllFolders(): List<FolderInfo>

    @Query("SELECT * FROM folder_info WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): FolderInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPreference(appPreference: AppPreference)

    @Update
    suspend fun updateAppPreference(appPreference: AppPreference)

    @Query("SELECT * FROM app_preferences WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppPreference(packageName: String): AppPreference?

    @Query("SELECT * FROM app_preferences WHERE isHidden = 1")
    suspend fun getHiddenApps(): List<AppPreference>

    @Query("SELECT * FROM app_preferences WHERE isLocked = 1")
    suspend fun getLockedApps(): List<AppPreference>
}
