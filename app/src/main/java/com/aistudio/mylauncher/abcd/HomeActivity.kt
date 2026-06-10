package com.aistudio.mylauncher.abcd

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val label: CharSequence,
    val resolveInfo: ResolveInfo,
    val packageName: String,
    val activityName: String
)

class HomeActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dockContainer: LinearLayout
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        
        // Status bar and nav bar should remain visible
        window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)
        window.setBackgroundDrawableResource(android.R.color.darker_gray)

        setContentView(R.layout.activity_home)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        dockContainer = findViewById(R.id.dockContainer)

        val fabSwitchLauncher = findViewById<android.widget.ImageButton>(R.id.fabSwitchLauncher)
        fabSwitchLauncher.setOnClickListener {
            android.util.Log.d("LauncherFAB", "FAB tapped")
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        fabSwitchLauncher.setOnLongClickListener {
            android.util.Log.d("LauncherFAB", "FAB long pressed")
            startActivity(Intent(this, LogViewerActivity::class.java))
            true
        }

        setupWallpaperBackground()
        loadInstalledApps()

        // Override back button (do nothing)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing
            }
        })
    }

    private fun setupWallpaperBackground() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            if (wallpaperDrawable != null) {
                window.decorView.background = wallpaperDrawable
            } else {
                window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)
            }
        } catch (e: Exception) {
            window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)
            e.printStackTrace()
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val loadedApps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
                resolveInfos.map { resolveInfo ->
                    AppInfo(
                        label = resolveInfo.loadLabel(packageManager),
                        resolveInfo = resolveInfo,
                        packageName = resolveInfo.activityInfo.packageName,
                        activityName = resolveInfo.activityInfo.name
                    )
                }.sortedBy { it.label.toString() }
            }
            allApps.clear()
            allApps.addAll(loadedApps)
            setupRecyclerView()
            setupDock()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.adapter = AppGridAdapter(allApps) { appInfo ->
            launchApp(appInfo)
        }
    }

    private fun setupDock() {
        val dockApps = allApps.take(4)
        for (app in dockApps) {
            val dockIconView = layoutInflater.inflate(R.layout.item_dock_icon, dockContainer, false)
            val iconImage = dockIconView.findViewById<ImageView>(R.id.appIcon)
            iconImage.setImageDrawable(app.resolveInfo.loadIcon(packageManager))
            dockIconView.setOnClickListener {
                launchApp(app)
            }
            dockContainer.addView(dockIconView)
        }
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class AppGridAdapter(
        private val apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)

            init {
                view.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onClick(apps[position])
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_icon, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.resolveInfo.loadIcon(packageManager))
            holder.name.text = app.label
        }

        override fun getItemCount() = apps.size
    }
}
