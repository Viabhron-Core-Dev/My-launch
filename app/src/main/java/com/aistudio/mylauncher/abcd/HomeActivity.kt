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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import android.content.Context
// Removed ViewPager2 import
import android.graphics.drawable.GradientDrawable

data class AppInfo(
    val label: CharSequence,
    val resolveInfo: ResolveInfo,
    val packageName: String,
    val activityName: String
)

class HomeActivity : ComponentActivity() {

    private lateinit var workspace: Workspace
    private lateinit var pageIndicatorContainer: LinearLayout
    private lateinit var dockContainer: LinearLayout
    private val allApps = mutableListOf<AppInfo>()

    private lateinit var drawerRoot: View
    private lateinit var drawerRecyclerView: RecyclerView
    private lateinit var drawerSearchInput: EditText
    private var isDrawerOpen = false
    private val drawerApps = mutableListOf<AppInfo>()
    private val hiddenAppsCache = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.d("HomeActivity", "STARTED: HomeActivity")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.activity_home)

        workspace = findViewById(R.id.workspace)
        pageIndicatorContainer = findViewById(R.id.pageIndicatorContainer)
        dockContainer = findViewById(R.id.dockContainer)

        ViewCompat.setOnApplyWindowInsetsListener(dockContainer) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navBarHeight)
            insets
        }

        val fabSwitchLauncher = findViewById<android.widget.ImageButton>(R.id.fabSwitchLauncher)
        fabSwitchLauncher.setOnClickListener {
            AppLogger.d("HomeActivity", "FAB tapped, opening Settings")
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        fabSwitchLauncher.setOnLongClickListener {
            AppLogger.d("HomeActivity", "FAB long pressed, opening Log Viewer")
            startActivity(Intent(this, LogViewerActivity::class.java))
            true
        }

        setupWallpaperBackground()
        loadInstalledApps()
        setupDrawer()

        // Override back button (do nothing)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDrawerOpen) {
                    closeDrawer()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AppLogger.d("HomeActivity", "RESUMED: HomeActivity")
        val sharedPrefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val gridCols = sharedPrefs.getInt("grid_columns", 4)
        val gridRows = sharedPrefs.getInt("grid_rows", 8)
        val lastCols = sharedPrefs.getInt("last_grid_columns", 4)
        val lastRows = sharedPrefs.getInt("last_grid_rows", 8)

        if (gridCols != lastCols || gridRows != lastRows) {
            AppLogger.d("Seeding", "Grid dimensions changed from ${lastCols}x${lastRows} to ${gridCols}x${gridRows}. Re-seeding home screen.")
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val db = LauncherDatabase.getDatabase(this@HomeActivity)
                    db.workspaceDao().clearHomeScreen()
                    sharedPrefs.edit()
                        .putInt("last_grid_columns", gridCols)
                        .putInt("last_grid_rows", gridRows)
                        .apply()
                }
                loadInstalledApps() // Reloads and re-seeds
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AppLogger.d("HomeActivity", "PAUSED: HomeActivity")
    }

    private fun setupDrawer() {
        drawerRoot = findViewById(R.id.appDrawerInclude)
        drawerRecyclerView = findViewById(R.id.drawerRecyclerView)
        drawerSearchInput = findViewById(R.id.drawerSearchInput)

        drawerRecyclerView.layoutManager = GridLayoutManager(this, 4)

        drawerRoot.post {
            drawerRoot.translationY = drawerRoot.height.toFloat()
        }

        drawerSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDrawerApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        drawerSearchInput.setOnClickListener { }
        drawerRecyclerView.setOnClickListener { }
    }

    private fun openDrawer() {
        AppLogger.d("Drawer", "Drawer opened")
        isDrawerOpen = true
        drawerSearchInput.setText("")
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@HomeActivity)
            val hiddenPrefs = db.workspaceDao().getHiddenApps()
            val hiddenPackageNames = hiddenPrefs.map { it.packageName }.toSet()
            
            withContext(Dispatchers.Main) {
                hiddenAppsCache.clear()
                hiddenAppsCache.addAll(hiddenPackageNames)
                filterDrawerApps("")
                
                drawerRoot.visibility = View.VISIBLE
                drawerRoot.animate()
                    .translationY(0f)
                    .setDuration(250)
                    .start()
            }
        }
    }

    private fun closeDrawer() {
        AppLogger.d("Drawer", "Drawer closed")
        isDrawerOpen = false
        drawerRoot.animate()
            .translationY(drawerRoot.height.toFloat())
            .setDuration(250)
            .withEndAction {
                drawerRoot.visibility = View.INVISIBLE
            }
            .start()
        
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun filterDrawerApps(query: String) {
        val q = query.lowercase()
        drawerApps.clear()
        
        val visibleApps = allApps.filter { it.packageName !in hiddenAppsCache }
        
        if (q.isEmpty()) {
            drawerApps.addAll(visibleApps)
        } else {
            drawerApps.addAll(visibleApps.filter {
                it.label.toString().lowercase().contains(q)
            })
        }
        if (drawerRecyclerView.adapter == null) {
            drawerRecyclerView.adapter = AppGridAdapter(drawerApps) { appInfo ->
                launchApp(appInfo)
                closeDrawer()
            }
        } else {
            drawerRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun setupWallpaperBackground() {
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            val customPath = prefs.getString("custom_wallpaper_path", null)
            if (customPath != null) {
                val file = java.io.File(customPath)
                if (file.exists()) {
                    try {
                        val options = android.graphics.BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        android.graphics.BitmapFactory.decodeFile(customPath, options)
                        
                        val metrics = resources.displayMetrics
                        val reqWidth = metrics.widthPixels
                        val reqHeight = metrics.heightPixels
                        
                        var inSampleSize = 1
                        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                            val halfHeight: Int = options.outHeight / 2
                            val halfWidth: Int = options.outWidth / 2
                            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                                inSampleSize *= 2
                            }
                        }
                        
                        options.inJustDecodeBounds = false
                        options.inSampleSize = inSampleSize
                        val bitmap = android.graphics.BitmapFactory.decodeFile(customPath, options)
                        if (bitmap != null) {
                            val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                            withContext(Dispatchers.Main) {
                                AppLogger.d("Wallpaper", "Loaded custom wallpaper from $customPath")
                                window.decorView.background = drawable
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Wallpaper", "Failed to load custom wallpaper", e)
                    }
                } else {
                    AppLogger.d("Wallpaper", "Custom wallpaper path set but file not found")
                }
            }
            
            withContext(Dispatchers.Main) {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(this@HomeActivity)
                    val wallpaperDrawable = wallpaperManager.drawable
                    if (wallpaperDrawable != null) {
                        AppLogger.d("Wallpaper", "Loaded system wallpaper via WallpaperManager")
                        window.decorView.background = wallpaperDrawable
                    } else {
                        AppLogger.d("Wallpaper", "WallpaperManager returned null, falling back to DKGRAY")
                        window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Wallpaper", "WallpaperManager failed, falling back to DKGRAY", e)
                    window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)
                }
            }
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
            AppLogger.d("HomeActivity", "Loaded ${loadedApps.size} installed apps")

            val appInfoMap = loadedApps.associateBy { Pair(it.packageName, it.activityName) }

            withContext(Dispatchers.IO) {
                val db = LauncherDatabase.getDatabase(this@HomeActivity)
                val dao = db.workspaceDao()
                val sharedPrefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                val gridCols = sharedPrefs.getInt("grid_columns", 4)
                val gridRows = sharedPrefs.getInt("grid_rows", 8)

                var items = dao.getAllForContainer(0)
                if (items.isEmpty()) {
                    AppLogger.d("Seeding", "Home screen is empty, seeding default apps")
                    val newItems = mutableListOf<WorkspaceItem>()
                    val page0Apps = listOf(
                        "com.android.chrome",
                        "com.anthropic.claude",
                        "com.whatsapp",
                        "com.android.vending",
                        "com.google.android.apps.docs",
                        "ai.perplexity.app.android",
                        "com.facebook.stella"
                    )
                    val page1Apps = listOf(
                        "org.mozilla.firefox"
                    )

                    var cX = 0
                    var cY = 0
                    for (pkg in page0Apps) {
                        val app = loadedApps.find { it.packageName == pkg }
                        if (app != null) {
                            newItems.add(
                                WorkspaceItem(
                                    itemType = 0,
                                    container = 0,
                                    containerId = -1L,
                                    packageName = app.packageName,
                                    activityName = app.activityName,
                                    title = app.label.toString(),
                                    customIconPath = "",
                                    appWidgetId = -1,
                                    spanX = 1,
                                    spanY = 1,
                                    cellX = cX,
                                    cellY = cY,
                                    pageIndex = 0
                                )
                            )
                            cX++
                            if (cX >= gridCols) {
                                cX = 0
                                cY++
                            }
                        }
                    }

                    cX = 0
                    cY = 0
                    for (pkg in page1Apps) {
                        val app = loadedApps.find { it.packageName == pkg }
                        if (app != null) {
                            newItems.add(
                                WorkspaceItem(
                                    itemType = 0,
                                    container = 0,
                                    containerId = -1L,
                                    packageName = app.packageName,
                                    activityName = app.activityName,
                                    title = app.label.toString(),
                                    customIconPath = "",
                                    appWidgetId = -1,
                                    spanX = 1,
                                    spanY = 1,
                                    cellX = cX,
                                    cellY = cY,
                                    pageIndex = 1
                                )
                            )
                            cX++
                            if (cX >= gridCols) {
                                cX = 0
                                cY++
                            }
                        }
                    }
                    
                    newItems.forEach { dao.insert(it) }
                    items = dao.getAllForContainer(0)
                    AppLogger.d("Seeding", "Seeded ${page0Apps.size} apps to Page 0, ${page1Apps.size} apps to Page 1")
                } else {
                    AppLogger.d("Seeding", "Home screen already contains ${items.size} items, skipping seed")
                }

                sharedPrefs.edit()
                    .putInt("last_grid_columns", gridCols)
                    .putInt("last_grid_rows", gridRows)
                    .apply()

                val pagesMap = items.groupBy { it.pageIndex }
                val maxPage = if (pagesMap.isEmpty()) 0 else pagesMap.keys.maxOrNull() ?: 0
                val pageItemsList = mutableListOf<List<WorkspaceItem?>>()
                val pageSize = gridCols * gridRows

                for (p in 0..maxPage) {
                    val pageItems = MutableList<WorkspaceItem?>(pageSize) { null }
                    pagesMap[p]?.forEach { item ->
                        val index = item.cellY * gridCols + item.cellX
                        if (index in 0 until pageSize) {
                            pageItems[index] = item
                        }
                    }
                    pageItemsList.add(pageItems)
                }

                withContext(Dispatchers.Main) {
                    setupWorkspace(pageItemsList, appInfoMap, gridCols, gridRows)
                    setupDock()
                }
            }
        }
    }

    private fun setupWorkspace(
        pageItemsList: List<List<WorkspaceItem?>>,
        appInfoMap: Map<Pair<String, String>, AppInfo>,
        gridCols: Int,
        gridRows: Int
    ) {
        workspace.removeAllViews()

        for (pageIndex in pageItemsList.indices) {
            val cellLayout = CellLayout(this, gridCols, gridRows)
            val itemsForPage = pageItemsList[pageIndex]

            for (itemIndex in itemsForPage.indices) {
                val item = itemsForPage[itemIndex]
                if (item != null) {
                    val appInfoKey = Pair(item.packageName, item.activityName)
                    val appInfo = appInfoMap[appInfoKey]

                    if (appInfo != null) {
                        val view = layoutInflater.inflate(R.layout.item_app_icon, cellLayout, false)
                        val iconView = view.findViewById<android.widget.ImageView>(R.id.appIcon)
                        val labelView = view.findViewById<android.widget.TextView>(R.id.appName)
                        labelView.visibility = View.GONE

                        val expectedIconPath = java.io.File(filesDir, "custom_icon_${item.packageName}.png")
                        if (expectedIconPath.exists()) {
                            iconView.setImageURI(android.net.Uri.fromFile(expectedIconPath))
                        } else {
                            iconView.setImageDrawable(appInfo.resolveInfo.loadIcon(packageManager))
                        }

                        labelView.text = item.title

                        view.setOnClickListener {
                            launchApp(appInfo)
                        }

                        cellLayout.addItemAt(view, item.cellX, item.cellY)
                    }
                }
            }
            
            cellLayout.onEmptyCellLongPressed = { cellX, cellY, touchX, touchY ->
                onEmptyCellLongPressed(pageIndex, cellX, cellY, cellLayout, touchX, touchY)
            }
            
            workspace.addView(cellLayout)
        }

        workspace.post {
            val actualWorkspaceHeight = workspace.height
            val actualWorkspaceWidth = workspace.width
            val cellWidth = actualWorkspaceWidth / gridCols
            val cellHeight = actualWorkspaceHeight / gridRows
            val iconSize = (minOf(cellWidth, cellHeight) * 0.75f).toInt()
            
            // Apply to all icon views across all CellLayouts
            for (i in 0 until workspace.childCount) {
                val cellLayout = workspace.getChildAt(i) as? CellLayout ?: continue
                for (j in 0 until cellLayout.childCount) {
                    val iconView = cellLayout.getChildAt(j)
                        ?.findViewById<android.widget.ImageView>(R.id.appIcon) ?: continue
                    val params = iconView.layoutParams
                    params.width = iconSize
                    params.height = iconSize
                    iconView.layoutParams = params
                    iconView.requestLayout()
                }
            }
        }

        setupPageIndicators(pageItemsList.size)
        workspace.onPageChanged = { position ->
            updatePageIndicators(position)
        }
        pageIndicatorContainer.visibility = View.VISIBLE
    }

    private fun setupPageIndicators(count: Int) {
        pageIndicatorContainer.removeAllViews()
        val dots = arrayOfNulls<ImageView>(count)
        for (i in 0 until count) {
            dots[i] = ImageView(this)
            val size = resources.displayMetrics.density * 8
            val margin = resources.displayMetrics.density * 4
            val params = LinearLayout.LayoutParams(size.toInt(), size.toInt())
            params.setMargins(margin.toInt(), 0, margin.toInt(), 0)
            dots[i]?.layoutParams = params
            dots[i]?.setImageDrawable(createDotDrawable(i == 0))
            pageIndicatorContainer.addView(dots[i])
        }
    }

    private fun updatePageIndicators(position: Int) {
        val count = pageIndicatorContainer.childCount
        for (i in 0 until count) {
            val dot = pageIndicatorContainer.getChildAt(i) as ImageView
            dot.setImageDrawable(createDotDrawable(i == position))
        }
    }

    private fun createDotDrawable(isActive: Boolean): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(if (isActive) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY)
        drawable.alpha = if (isActive) 255 else 128
        return drawable
    }

    private fun setupDock() {
        dockContainer.removeAllViews()

        var appIndex = 0
        val totalSlots = 5

        for (i in 0 until totalSlots) {
            if (i == 2) {
                // Center toggle
                val dockIconView = layoutInflater.inflate(R.layout.item_dock_icon, dockContainer, false)
                val iconImage = dockIconView.findViewById<ImageView>(R.id.appIcon)
                iconImage.setImageResource(android.R.drawable.ic_menu_more)
                dockIconView.setOnClickListener {
                    openDrawer()
                }
                dockContainer.addView(dockIconView)
            } else {
                if (appIndex < allApps.size) {
                    val app = allApps[appIndex]
                    appIndex++
                    
                    val dockIconView = layoutInflater.inflate(R.layout.item_dock_icon, dockContainer, false)
                    val iconImage = dockIconView.findViewById<ImageView>(R.id.appIcon)
                    iconImage.setImageDrawable(app.resolveInfo.loadIcon(packageManager))
                    dockIconView.setOnClickListener {
                        launchApp(app)
                    }
                    dockContainer.addView(dockIconView)
                }
            }
        }
        AppLogger.d("HomeActivity", "Dock setup completed with $totalSlots slots")
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                AppLogger.d("HomeActivity", "Launching app: ${app.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                AppLogger.d("HomeActivity", "Launch intent null for: ${app.packageName}")
            }
        } catch (e: Exception) {
            AppLogger.e("HomeActivity", "Failed to launch app: ${app.packageName}", e)
        }
    }

    private fun onAppDropped(appInfo: AppInfo, pageIndex: Int, cellX: Int, cellY: Int) {
        AppLogger.d("Drag", "App ${appInfo.packageName} dropped at page=$pageIndex cellX=$cellX cellY=$cellY")
        lifecycleScope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@HomeActivity)
            val dao = db.workspaceDao()
            dao.insert(
                WorkspaceItem(
                    itemType = 0,
                    container = 0,
                    containerId = -1L,
                    packageName = appInfo.packageName,
                    activityName = appInfo.activityName,
                    title = appInfo.label.toString(),
                    customIconPath = "",
                    appWidgetId = -1,
                    spanX = 1,
                    spanY = 1,
                    cellX = cellX,
                    cellY = cellY,
                    pageIndex = pageIndex
                )
            )
            withContext(Dispatchers.Main) {
                loadInstalledApps() // reloads and refreshes home grid
            }
        }
    }

    private fun onEmptyCellLongPressed(
        pageIndex: Int, cellX: Int, cellY: Int, 
        cellLayout: CellLayout, touchX: Float, touchY: Float
    ) {
        // Create temporary invisible anchor view at touch position
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)
        val anchor = View(this).apply {
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(1, 1)
            x = touchX + cellLayout.x + (pageIndex * workspace.width) - workspace.scrollX
            y = touchY + cellLayout.y + workspace.y
            visibility = View.INVISIBLE
        }
        rootLayout.addView(anchor)
        
        val popupMenu = android.widget.PopupMenu(this, anchor)
        popupMenu.menu.add(0, 0, 0, "Add App")
        popupMenu.menu.add(0, 1, 1, "Add Folder")
        popupMenu.menu.add(0, 2, 2, "Add Widget")
        popupMenu.setOnDismissListener { rootLayout.removeView(anchor) }
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { showAppPicker(pageIndex, cellX, cellY); true }
                1 -> { android.widget.Toast.makeText(this, "Folders coming in a future update", android.widget.Toast.LENGTH_SHORT).show(); true }
                2 -> { android.widget.Toast.makeText(this, "Widgets coming in a future update", android.widget.Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
        popupMenu.show()
        AppLogger.d("HomeScreen", "Empty cell long-press menu shown at page=$pageIndex cellX=$cellX cellY=$cellY")
    }

    private fun showAppPicker(pageIndex: Int, cellX: Int, cellY: Int) {
        val visibleApps = allApps.filter { it.packageName !in hiddenAppsCache }
        
        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@HomeActivity, 4)
            setPadding(32, 32, 32, 32)
        }
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Select App")
            .setView(recyclerView)
            .create()

        recyclerView.adapter = AppGridAdapter(visibleApps) { appInfo ->
            dialog.dismiss()
            
            AppLogger.d("HomeScreen", "App ${appInfo.packageName} added via long-press picker at page=$pageIndex cellX=$cellX cellY=$cellY")
            lifecycleScope.launch(Dispatchers.IO) {
                val db = LauncherDatabase.getDatabase(this@HomeActivity)
                val dao = db.workspaceDao()
                dao.insert(
                    WorkspaceItem(
                        itemType = 0,
                        container = 0,
                        containerId = -1L,
                        packageName = appInfo.packageName,
                        activityName = appInfo.activityName,
                        title = appInfo.label.toString(),
                        customIconPath = "",
                        appWidgetId = -1,
                        spanX = 1,
                        spanY = 1,
                        cellX = cellX,
                        cellY = cellY,
                        pageIndex = pageIndex
                    )
                )
                withContext(Dispatchers.Main) {
                    loadInstalledApps() // reloads and refreshes home grid
                }
            }
        }
        
        dialog.show()
    }

    private fun showAppContextMenu(app: AppInfo, anchorView: View) {
        AppLogger.d("Drawer", "Long-press context menu shown for ${app.packageName}")
        val popupMenu = android.widget.PopupMenu(this, anchorView)
        popupMenu.menu.add(0, 0, 0, "Add to home screen")
        popupMenu.menu.add(0, 1, 1, "App info")
        popupMenu.menu.add(0, 2, 2, "Uninstall")
        popupMenu.menu.add(0, 3, 3, "Pause")
        popupMenu.menu.add(0, 4, 4, "Hide from drawer")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { // Add to home screen
                    AppLogger.d("Drag", "Add to home screen requested for ${app.packageName}")
                    val clipData = android.content.ClipData.newPlainText("app_package", app.packageName)
                    val shadowBuilder = View.DragShadowBuilder(anchorView)
                    anchorView.startDragAndDrop(clipData, shadowBuilder, app, 0)
                    closeDrawer()
                    true
                }
                1 -> { // App info
                    AppLogger.d("Drawer", "App info requested for ${app.packageName}")
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", app.packageName, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        AppLogger.e("Drawer", "Failed to open app info for ${app.packageName}", e)
                    }
                    closeDrawer()
                    true
                }
                2 -> { // Uninstall
                    AppLogger.d("Drawer", "Uninstall requested for ${app.packageName}")
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = android.net.Uri.fromParts("package", app.packageName, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        AppLogger.e("Drawer", "Failed to uninstall ${app.packageName}", e)
                    }
                    closeDrawer()
                    true
                }
                3 -> { // Pause
                    AppLogger.d("Drawer", "Pause requested for ${app.packageName} — not implemented, requires device admin")
                    android.widget.Toast.makeText(this@HomeActivity, "Pause requires device admin — not yet available", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                4 -> { // Hide
                    AppLogger.d("Drawer", "Hide requested for ${app.packageName}")
                    hideAppFromDrawer(app)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun hideAppFromDrawer(app: AppInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = LauncherDatabase.getDatabase(this@HomeActivity)
                val dao = db.workspaceDao()
                var pref = dao.getAppPreference(app.packageName)
                if (pref == null) {
                    pref = AppPreference(
                        packageName = app.packageName,
                        isHidden = true,
                        customIconPath = "",
                        customLabel = ""
                    )
                } else {
                    pref = pref.copy(isHidden = true)
                }
                dao.insertAppPreference(pref)

                withContext(Dispatchers.Main) {
                    hiddenAppsCache.add(app.packageName)
                    filterDrawerApps(drawerSearchInput.text.toString())
                    AppLogger.d("Drawer", "${app.packageName} is now hidden from drawer")
                }
            } catch (e: Exception) {
                AppLogger.e("Drawer", "Failed to hide ${app.packageName}", e)
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
                view.setOnLongClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        showAppContextMenu(apps[position], view)
                    }
                    true
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
