package com.aistudio.mylauncher.abcd

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AppLogger {
    private const val MAX_FILE_SIZE = 1048576L // 1MB
    private val executor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun getLogFile(): File? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dir != null) {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return File(dir, "launcher_log.txt")
        }
        return null
    }

    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?) {
        val timeStamp = dateFormat.format(Date())
        val logLine = "[$timeStamp] [$level] [$tag] $message\n"
        var stackTrace = ""
        if (throwable != null) {
            stackTrace = Log.getStackTraceString(throwable) + "\n"
        }
        
        executor.execute {
            try {
                val file = getLogFile() ?: return@execute
                
                if (file.exists() && file.length() > MAX_FILE_SIZE) {
                    truncateFile(file)
                }

                FileWriter(file, true).use { fw ->
                    fw.append(logLine)
                    if (stackTrace.isNotEmpty()) {
                        fw.append(stackTrace)
                    }
                }
            } catch (e: Exception) {
                // Silently drop if file write fails, adb log is already written
            }
        }
    }

    private fun truncateFile(file: File) {
        try {
            val content = file.readText()
            val midpoint = content.length / 2
            val newlineIndex = content.indexOf('\n', midpoint)
            val truncatedContent = if (newlineIndex != -1 && newlineIndex + 1 < content.length) {
                content.substring(newlineIndex + 1)
            } else {
                content.substring(midpoint)
            }
            file.writeText(truncatedContent)
        } catch (e: Exception) {
            // Ignore truncate errors
        }
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeToFile("DEBUG", tag, message, null)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        writeToFile("ERROR", tag, message, throwable)
    }
}
