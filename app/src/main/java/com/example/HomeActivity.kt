package com.example

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

data class AppInfo(
    val label: CharSequence,
    val icon: Drawable,
    val packageName: String,
    val activityName: String
)

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dockContainer: LinearLayout
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide navigation bar and status bar for a clean launcher experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)
        dockContainer = findViewById(R.id.dockContainer)

        setupWallpaperBackground()
        loadInstalledApps()
        setupViewPager()
        setupDock()

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
            window.decorView.background = wallpaperDrawable
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadInstalledApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfos) {
            allApps.add(
                AppInfo(
                    label = resolveInfo.loadLabel(packageManager),
                    icon = resolveInfo.loadIcon(packageManager),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name
                )
            )
        }
        // Basic sort
        allApps.sortBy { it.label.toString() }
    }

    private fun setupViewPager() {
        // 3 pages
        val pagesCount = 3
        val appsPerPage = 20 // 4 cols x 5 rows

        viewPager.adapter = object : RecyclerView.Adapter<PageViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
                val rv = RecyclerView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    layoutManager = GridLayoutManager(parent.context, 4)
                    // Optional padding
                    setPadding(16, 32, 16, 16)
                    clipToPadding = false
                }
                return PageViewHolder(rv)
            }

            override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
                val startIndex = position * appsPerPage
                val endIndex = minOf(startIndex + appsPerPage, allApps.size)
                val pageApps = if (startIndex < allApps.size) {
                    allApps.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                holder.bind(pageApps)
            }

            override fun getItemCount() = pagesCount
        }
    }

    private fun setupDock() {
        val dockApps = allApps.take(4)
        for (app in dockApps) {
            val dockIconView = layoutInflater.inflate(R.layout.item_dock_icon, dockContainer, false)
            val iconImage = dockIconView.findViewById<ImageView>(R.id.appIcon)
            iconImage.setImageDrawable(app.icon)
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

    inner class PageViewHolder(private val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView) {
        fun bind(apps: List<AppInfo>) {
            recyclerView.adapter = AppGridAdapter(apps) { appInfo ->
                launchApp(appInfo)
            }
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
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.label
        }

        override fun getItemCount() = apps.size
    }
}
