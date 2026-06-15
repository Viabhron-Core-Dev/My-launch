package com.aistudio.mylauncher.abcd

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.app.AlertDialog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {

    private val wallpaperPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val file = File(filesDir, "custom_wallpaper.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("custom_wallpaper_path", file.absolutePath)
                        .apply()
                        
                    AppLogger.d("SettingsActivity", "Custom wallpaper set to ${file.absolutePath}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Custom wallpaper set. Returning to home...", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLogger.d("SettingsActivity", "RESUMED: SettingsActivity")
    }

    override fun onPause() {
        super.onPause()
        AppLogger.d("SettingsActivity", "PAUSED: SettingsActivity")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("SettingsActivity", "STARTED: SettingsActivity")
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContentView(R.layout.activity_settings)

        loadHiddenApps()

        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

        // Grid Columns
        val textColumns = findViewById<TextView>(R.id.textColumns)
        val seekColumns = findViewById<SeekBar>(R.id.seekColumns)
        val columns = prefs.getInt("grid_columns", 4)
        seekColumns.max = 3 // 0 to 3 corresponds to 3 to 6
        seekColumns.progress = columns - 3
        textColumns.text = "Grid columns: $columns"
        seekColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 3
                textColumns.text = "Grid columns: $value"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = (seekBar?.progress ?: 0) + 3
                AppLogger.d("SettingsActivity", "Grid columns changed to: $value")
                prefs.edit().putInt("grid_columns", value).apply()
            }
        })

        // Grid Rows
        val textRows = findViewById<TextView>(R.id.textRows)
        val seekRows = findViewById<SeekBar>(R.id.seekRows)
        val rows = prefs.getInt("grid_rows", 5)
        seekRows.max = 4 // 0 to 4 corresponds to 4 to 8
        seekRows.progress = rows - 4
        textRows.text = "Grid rows per page: $rows"
        seekRows.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 4
                textRows.text = "Grid rows per page: $value"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = (seekBar?.progress ?: 0) + 4
                AppLogger.d("SettingsActivity", "Grid rows changed to: $value")
                prefs.edit().putInt("grid_rows", value).apply()
            }
        })

        // Dock Icon Count
        val rgDockCount = findViewById<RadioGroup>(R.id.rgDockCount)
        val dockCount = prefs.getInt("dock_count", 4)
        if (dockCount == 5) {
            findViewById<RadioButton>(R.id.rbDock5).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.rbDock4).isChecked = true
        }
        rgDockCount.setOnCheckedChangeListener { _, checkedId ->
            val count = if (checkedId == R.id.rbDock5) 5 else 4
            AppLogger.d("SettingsActivity", "Dock count changed to: $count")
            prefs.edit().putInt("dock_count", count).apply()
        }

        // Dock Divider
        val switchDockDivider = findViewById<Switch>(R.id.switchDockDivider)
        switchDockDivider.isChecked = prefs.getBoolean("dock_divider", false)
        switchDockDivider.setOnCheckedChangeListener { _, isChecked ->
            AppLogger.d("SettingsActivity", "Dock divider changed to: $isChecked")
            prefs.edit().putBoolean("dock_divider", isChecked).apply()
        }

        // Icon Size
        val rgIconSize = findViewById<RadioGroup>(R.id.rgIconSize)
        val iconSize = prefs.getString("icon_size", "medium") ?: "medium"
        when (iconSize) {
            "small" -> findViewById<RadioButton>(R.id.rbIconSmall).isChecked = true
            "large" -> findViewById<RadioButton>(R.id.rbIconLarge).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbIconMedium).isChecked = true
        }
        rgIconSize.setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                R.id.rbIconSmall -> "small"
                R.id.rbIconLarge -> "large"
                else -> "medium"
            }
            AppLogger.d("SettingsActivity", "Icon size changed to: $size")
            prefs.edit().putString("icon_size", size).apply()
        }

        // About - Version
        val textVersion = findViewById<TextView>(R.id.textVersion)
        textVersion.text = "App version: ${BuildConfig.VERSION_NAME}"

        // Clear Setup Flag
        val btnClearSetup = findViewById<Button>(R.id.btnClearSetup)
        btnClearSetup.setOnClickListener {
            AppLogger.d("SettingsActivity", "Clear setup flag clicked")
            // using "app_prefs" per WelcomeActivity
            val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            appPrefs.edit().remove("setup_complete").apply()
            
            // Finish this activity to go back to HomeActivity
            finish()
        }

        val btnClearHomeScreen = findViewById<Button>(R.id.btnClearHomeScreen)
        btnClearHomeScreen.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear home screen?")
                .setMessage("All home screen icons will be removed. Apps remain in the drawer.")
                .setPositiveButton("Clear") { _, _ ->
                    AppLogger.d("SettingsActivity", "Clear home screen confirmed")
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = LauncherDatabase.getDatabase(this@SettingsActivity)
                        db.workspaceDao().clearHomeScreen()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val btnSetWallpaper = findViewById<Button>(R.id.btnSetWallpaper)
        btnSetWallpaper.setOnClickListener {
            AppLogger.d("SettingsActivity", "Set Wallpaper clicked")
            wallpaperPickerLauncher.launch("image/*")
        }
        btnSetWallpaper.setOnLongClickListener {
            val launcherPrefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            val path = launcherPrefs.getString("custom_wallpaper_path", null)
            if (path != null) {
                File(path).delete()
                launcherPrefs.edit().remove("custom_wallpaper_path").apply()
                AppLogger.d("SettingsActivity", "Custom wallpaper cleared")
                Toast.makeText(this, "Custom wallpaper cleared", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No custom wallpaper to clear", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun loadHiddenApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@SettingsActivity)
            val dao = db.workspaceDao()
            val hiddenApps = dao.getHiddenApps()
            
            withContext(Dispatchers.Main) {
                val container = findViewById<android.widget.LinearLayout>(R.id.containerHiddenApps)
                container.removeAllViews()
                
                if (hiddenApps.isEmpty()) {
                    val tv = android.widget.TextView(this@SettingsActivity).apply {
                        text = "No hidden apps"
                        setTextColor(android.graphics.Color.WHITE)
                    }
                    container.addView(tv)
                } else {
                    val pm = packageManager
                    for (appTarget in hiddenApps) {
                        try {
                            val ai = pm.getApplicationInfo(appTarget.packageName, 0)
                            val label = pm.getApplicationLabel(ai).toString()
                            
                            val view = layoutInflater.inflate(R.layout.item_hidden_app, container, false)
                            view.findViewById<android.widget.TextView>(R.id.hiddenAppName).text = label
                            view.findViewById<android.widget.Button>(R.id.btnUnhide).setOnClickListener {
                                AppLogger.d("SettingsActivity", "Unhiding ${appTarget.packageName}")
                                unhideApp(appTarget)
                            }
                            container.addView(view)
                        } catch (e: Exception) {
                            // package not found
                            AppLogger.e("SettingsActivity", "Package not found for hidden app: ${appTarget.packageName}", e)
                        }
                    }
                }
            }
        }
    }

    private fun unhideApp(appPref: AppPreference) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@SettingsActivity)
            val dao = db.workspaceDao()
            dao.updateAppPreference(appPref.copy(isHidden = false))
            loadHiddenApps()
        }
    }
}
