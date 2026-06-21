package com.aistudio.mylauncher.abcd

import android.content.Context
import android.view.View
import android.view.ViewGroup

class CellLayout(
    context: Context,
    private val gridColumns: Int,
    private val gridRows: Int
) : ViewGroup(context) {

    class LayoutParams(
        val cellX: Int,
        val cellY: Int,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.MATCH_PARENT
    ) : ViewGroup.LayoutParams(width, height)

    fun addItemAt(view: View, cellX: Int, cellY: Int) {
        val params = LayoutParams(cellX, cellY)
        view.layoutParams = params
        addView(view)
    }

    fun clear() {
        removeAllViews()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val cellWidth = if (gridColumns > 0) width / gridColumns else 0
        val cellHeight = if (gridRows > 0) height / gridRows else 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val childWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY)
                val childHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY)
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        
        val cellWidth = if (gridColumns > 0) width / gridColumns else 0
        val cellHeight = if (gridRows > 0) height / gridRows else 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as LayoutParams
                val childLeft = lp.cellX * cellWidth
                val childTop = lp.cellY * cellHeight
                child.layout(childLeft, childTop, childLeft + cellWidth, childTop + cellHeight)
            }
        }
    }
}
