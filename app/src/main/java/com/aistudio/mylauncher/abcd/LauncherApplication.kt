package com.aistudio.mylauncher.abcd

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherApplication : Application() {

    companion object {
        const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    }

    override fun onCreate() {
        try {
            super.onCreate()
            setupCrashHandler()
            setupActivityRecorder()
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            val fallbackDir = File("/data/data/com.aistudio.mylauncher.abcd/files/")
            val dir = try { filesDir ?: fallbackDir } catch (ex: Exception) { fallbackDir }
            if (!dir.exists()) dir.mkdirs()
            File(dir, "launcher_crash_latest.txt").writeText("Application onCreate Crash:\n$sw")
            throw e
        }
    }
    
    private fun logEvent(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logLine = "[$timestamp] $message\n"
        val fallbackDir = File("/data/data/com.aistudio.mylauncher.abcd/files/")
        val dir = try { filesDir ?: fallbackDir } catch (ex: Exception) { fallbackDir }
        if (!dir.exists()) dir.mkdirs()
        val logFile = File(dir, "launcher_log.txt")
        
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                // Read all lines, keep second half, rewrite
                val lines = logFile.readLines()
                val halfLines = lines.subList(lines.size / 2, lines.size)
                logFile.printWriter().use { out ->
                    halfLines.forEach { out.println(it) }
                }
            }
            logFile.appendText(logLine)
        } catch (e: Exception) {
            Log.e("LauncherCrash", "Failed to write log", e)
        }
    }

    private fun setupActivityRecorder() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                logEvent("STARTED: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fallbackDir = File("/data/data/com.aistudio.mylauncher.abcd/files/")
                val dir = try { filesDir ?: fallbackDir } catch (ex: Exception) { fallbackDir }
                
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                val crashFile = File(dir, "launcher_crash_$timestamp.txt")
                val latestCrashFile = File(dir, "launcher_crash_latest.txt")
                
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                exception.printStackTrace(pw)
                val stackTrace = sw.toString()
                
                val version = try {
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: Exception) {
                    "Unknown"
                }
                
                val logFile = File(dir, "launcher_log.txt")
                val recentLogs = if (logFile.exists()) {
                    val lines = logFile.readLines()
                    lines.takeLast(50).joinToString("\n")
                } else {
                    "No recent log available."
                }
                
                val crashLogContent = """
                    |Timestamp: $timestamp
                    |Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    |Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    |App Version: $version
                    |
                    |Recent Logs (Last 50 Lines):
                    |$recentLogs
                    |
                    |Stack Trace:
                    |$stackTrace
                """.trimMargin()
                
                crashFile.writeText(crashLogContent)
                latestCrashFile.writeText(crashLogContent)
            } catch (e: Exception) {
                Log.e("LauncherCrash", "Failed to write crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
}
