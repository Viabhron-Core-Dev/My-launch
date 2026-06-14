package com.aistudio.mylauncher.abcd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomePageAdapter(
    private val pageItemsList: List<List<WorkspaceItem?>>,
    private val appInfoMap: Map<Pair<String, String>, AppInfo>,
    private val gridColumns: Int,
    private val gridRows: Int,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<HomePageAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view.findViewById(R.id.pageRecyclerView)

        init {
            recyclerView.layoutManager = GridLayoutManager(view.context, gridColumns)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val itemsForPage = pageItemsList[position]
        holder.recyclerView.adapter = HomeGridAdapter(itemsForPage, appInfoMap, gridRows, onClick)
    }

    override fun getItemCount(): Int = pageItemsList.size
}

class HomeGridAdapter(
    private val items: List<WorkspaceItem?>,
    private val appInfoMap: Map<Pair<String, String>, AppInfo>,
    private val gridRows: Int,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<HomeGridAdapter.CellViewHolder>() {

    inner class CellViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    if (item != null) {
                        val appInfo = appInfoMap[Pair(item.packageName, item.activityName)]
                        if (appInfo != null) {
                            onClick(appInfo)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        val holder = CellViewHolder(view)
        
        val setHeight = {
            if (parent.height > 0) {
                val cellHeight = parent.height / gridRows
                if (view.layoutParams.height != cellHeight) {
                    view.layoutParams.height = cellHeight
                    view.requestLayout()
                }
            }
        }
        
        if (parent.height > 0) {
            setHeight()
        } else {
            parent.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (parent.height > 0) {
                        parent.viewTreeObserver.removeOnPreDrawListener(this)
                        setHeight()
                    }
                    return true
                }
            })
        }

        return holder
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val item = items[position]
        if (item == null) {
            holder.itemView.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            holder.icon.setImageDrawable(null)
            holder.name.text = ""
        } else {
            val appInfo = appInfoMap[Pair(item.packageName, item.activityName)]
            if (appInfo != null) {
                holder.itemView.visibility = View.VISIBLE
                val pm = holder.itemView.context.packageManager
                holder.icon.setImageDrawable(appInfo.resolveInfo.loadIcon(pm))
                holder.name.text = appInfo.label
            } else {
                holder.itemView.visibility = View.INVISIBLE
                holder.itemView.setOnClickListener(null)
                holder.icon.setImageDrawable(null)
                holder.name.text = ""
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
