package com.aistudio.mylauncher.abcd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File

class LogViewerActivity : ComponentActivity() {

    private lateinit var tvLogContent: TextView
    private var currentLogFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("LogViewerActivity", "STARTED: LogViewerActivity")
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContentView(R.layout.activity_log_viewer)

        tvLogContent = findViewById(R.id.tvLogContent)
        
        val btnCrashLog = findViewById<Button>(R.id.btnCrashLog)
        val btnRunningLog = findViewById<Button>(R.id.btnRunningLog)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopyLog = findViewById<Button>(R.id.btnCopyLog)
        val btnSaveLog = findViewById<Button>(R.id.btnSaveLog)

        btnCrashLog.setOnClickListener {
            loadLogFile("launcher_crash_latest.txt")
        }

        btnRunningLog.setOnClickListener {
            loadLogFile("launcher_log.txt")
        }

        btnShare.setOnClickListener {
            AppLogger.d("LogViewerActivity", "Share Logs clicked")
            shareCurrentLog()
        }

        btnClear.setOnClickListener {
            AppLogger.d("LogViewerActivity", "Clear Logs clicked")
            clearLogs()
        }

        btnCopyLog.setOnClickListener {
            AppLogger.d("LogViewerActivity", "Copy Logs clicked")
            val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Launcher Log", tvLogContent.text)
            cm.setPrimaryClip(clip)
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnSaveLog.setOnClickListener {
            AppLogger.d("LogViewerActivity", "Save Logs clicked")
            val text = tvLogContent.text.toString()
            if (text.isNotBlank()) {
                val timestamp = System.currentTimeMillis()
                val filename = "launcher_log_$timestamp.txt"
                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (dir != null) {
                    val file = File(dir, filename)
                    file.writeText(text)
                    AppLogger.d("LogViewerActivity", "Logs saved to $filename")
                    Toast.makeText(this, "Saved: $filename", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not access Downloads directory", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            }
        }

        // Load running log by default
        loadLogFile("launcher_log.txt")
    }

    override fun onResume() {
        super.onResume()
        AppLogger.d("LogViewerActivity", "RESUMED: LogViewerActivity")
    }

    override fun onPause() {
        super.onPause()
        AppLogger.d("LogViewerActivity", "PAUSED: LogViewerActivity")
    }

    private fun loadLogFile(filename: String) {
        val filesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (filesDir != null) {
            val file = File(filesDir, filename)
            currentLogFile = file
            if (file.exists()) {
                tvLogContent.text = file.readText()
            } else {
                tvLogContent.text = "File not found: $filename\nThere are no logs of this type yet."
            }
        } else {
            tvLogContent.text = "Could not access internal files directory."
            currentLogFile = null
        }
    }

    private fun shareCurrentLog() {
        val file = currentLogFile
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Log File"))
        } else {
            Toast.makeText(this, "No log file to share.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearLogs() {
        val filesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (filesDir != null) {
            File(filesDir, "launcher_crash_latest.txt").delete()
            File(filesDir, "launcher_log.txt").delete()
            // Also clean up any timestamped crash files
            filesDir.listFiles { _, name -> name.startsWith("launcher_crash_") }?.forEach { it.delete() }
            tvLogContent.text = "Logs cleared successfully."
            currentLogFile = null
        } else {
            tvLogContent.text = "Could not access internal files directory to delete logs."
        }
    }
}
