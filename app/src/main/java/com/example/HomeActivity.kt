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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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

        val fabSwitchLauncher = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabSwitchLauncher)
        fabSwitchLauncher.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    startActivityForResult(intent, 0)
                }
            }
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
            window.decorView.background = wallpaperDrawable
        } catch (e: Exception) {
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
            setupViewPager()
            setupDock()
        }
    }

    private fun setupViewPager() {
        val appsPerPage = 20 // 4 cols x 5 rows
        val pagesCount = maxOf(1, (allApps.size + appsPerPage - 1) / appsPerPage)

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
            holder.icon.setImageDrawable(app.resolveInfo.loadIcon(packageManager))
            holder.name.text = app.label
        }

        override fun getItemCount() = apps.size
    }
}
