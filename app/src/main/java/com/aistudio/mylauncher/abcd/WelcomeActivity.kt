package com.aistudio.mylauncher.abcd

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var iconStep2: TextView
    private lateinit var btnStorage: Button
    
    private lateinit var iconStep3: TextView
    private lateinit var btnLauncher: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            if (sharedPrefs.getBoolean("setup_complete", false)) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return
            }

            setContentView(R.layout.activity_welcome)
            
            iconStep2 = findViewById(R.id.iconStep2)
            btnStorage = findViewById(R.id.btnStorage)
            
            iconStep3 = findViewById(R.id.iconStep3)
            btnLauncher = findViewById(R.id.btnLauncher)
            
            val btnContinue = findViewById<Button>(R.id.btnContinue)
            
            btnStorage.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
            }
            
            btnLauncher.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        startActivityForResult(intent, 1)
                    }
                }
            }
            
            btnContinue.setOnClickListener {
                sharedPrefs.edit().putBoolean("setup_complete", true).apply()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            try {
                val sw = java.io.StringWriter()
                e.printStackTrace(java.io.PrintWriter(sw))
                java.io.File(filesDir, "launcher_crash_latest.txt").writeText("WelcomeActivity onCreate crash:\n$sw")
            } catch (ignored: Exception) {}
            throw e
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
    
    private fun checkPermissions() {
        // Check Storage (Step 2)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                iconStep2.text = "✓"
                iconStep2.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                btnStorage.visibility = android.view.View.GONE
            } else {
                iconStep2.text = "2"
                iconStep2.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                btnStorage.text = "Retry Storage Access"
                btnStorage.visibility = android.view.View.VISIBLE
            }
        } else {
            // Below Android 11, MANAGE_EXTERNAL_STORAGE is not used
            iconStep2.text = "✓"
            iconStep2.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            btnStorage.visibility = android.view.View.GONE
        }
        
        // Check Launcher (Step 3)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                iconStep3.text = "✓"
                iconStep3.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                btnLauncher.visibility = android.view.View.GONE
            } else {
                iconStep3.text = "3"
                iconStep3.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                btnLauncher.text = "Retry Default Launcher"
                btnLauncher.visibility = android.view.View.VISIBLE
            }
        }
    }
}
