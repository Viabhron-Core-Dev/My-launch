package com.aistudio.mylauncher.abcd

import android.app.Application
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val crashFile = File(downloadsDir, "launcher_crash_$timestamp.txt")
                    PrintWriter(crashFile).use { writer ->
                        exception.printStackTrace(writer)
                    }
                }
            } catch (e: Exception) {
                Log.e("LauncherCrash", "Failed to write crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
}
