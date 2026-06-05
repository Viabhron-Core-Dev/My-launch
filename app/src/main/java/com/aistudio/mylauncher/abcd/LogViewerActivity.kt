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
        setContentView(R.layout.activity_log_viewer)

        tvLogContent = findViewById(R.id.tvLogContent)
        
        val btnCrashLog = findViewById<Button>(R.id.btnCrashLog)
        val btnRunningLog = findViewById<Button>(R.id.btnRunningLog)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val btnClear = findViewById<Button>(R.id.btnClear)

        btnCrashLog.setOnClickListener {
            loadLogFile("launcher_crash_latest.txt")
        }

        btnRunningLog.setOnClickListener {
            loadLogFile("launcher_log.txt")
        }

        btnShare.setOnClickListener {
            shareCurrentLog()
        }

        btnClear.setOnClickListener {
            clearLogs()
        }

        // Load running log by default
        loadLogFile("launcher_log.txt")
    }

    private fun loadLogFile(filename: String) {
        val filesDir = filesDir
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
        val filesDir = filesDir
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
