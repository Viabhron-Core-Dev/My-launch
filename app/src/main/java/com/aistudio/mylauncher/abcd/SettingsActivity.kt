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

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

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
                prefs.edit().putInt("grid_columns", value).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
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
                prefs.edit().putInt("grid_rows", value).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
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
            prefs.edit().putInt("dock_count", count).apply()
        }

        // Dock Divider
        val switchDockDivider = findViewById<Switch>(R.id.switchDockDivider)
        switchDockDivider.isChecked = prefs.getBoolean("dock_divider", false)
        switchDockDivider.setOnCheckedChangeListener { _, isChecked ->
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
            prefs.edit().putString("icon_size", size).apply()
        }

        // About - Version
        val textVersion = findViewById<TextView>(R.id.textVersion)
        textVersion.text = "App version: ${BuildConfig.VERSION_NAME}"

        // Clear Setup Flag
        val btnClearSetup = findViewById<Button>(R.id.btnClearSetup)
        btnClearSetup.setOnClickListener {
            // using "app_prefs" per WelcomeActivity
            val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            appPrefs.edit().remove("setup_complete").apply()
            
            // Finish this activity to go back to HomeActivity
            finish()
        }
    }
}
