package com.aistudio.mylauncher.abcd

import android.content.Context
import android.view.View
import android.view.ViewGroup

class CellLayout(
    context: Context,
    private val gridColumns: Int,
    private val gridRows: Int
) : ViewGroup(context) {

    private val padding = (4 * resources.displayMetrics.density).toInt()
    private val occupiedCells = mutableSetOf<Pair<Int, Int>>()
    
    var onEmptyCellLongPressed: ((cellX: Int, cellY: Int) -> Unit)? = null

    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: android.view.MotionEvent) {
            val cellWidth = if (gridColumns > 0) width / gridColumns else 1
            val cellHeight = if (gridRows > 0) height / gridRows else 1

            val cellX = (e.x / cellWidth).toInt().coerceIn(0, gridColumns - 1)
            val cellY = (e.y / cellHeight).toInt().coerceIn(0, gridRows - 1)

            if (!occupiedCells.contains(Pair(cellX, cellY))) {
                onEmptyCellLongPressed?.invoke(cellX, cellY)
            }
        }
    })

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        var handled = gestureDetector.onTouchEvent(event)
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            handled = true
        }
        return handled || super.onTouchEvent(event)
    }

    class LayoutParams(
        val cellX: Int,
        val cellY: Int,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.MATCH_PARENT
    ) : ViewGroup.LayoutParams(width, height)

    fun addItemAt(view: View, cellX: Int, cellY: Int) {
        val params = LayoutParams(cellX, cellY)
        view.layoutParams = params
        occupiedCells.add(Pair(cellX, cellY))
        addView(view)
    }

    fun clear() {
        occupiedCells.clear()
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
                child.layout(childLeft + padding, childTop + padding, childLeft + cellWidth - padding, childTop + cellHeight - padding)
            }
        }
    }
}
