package com.aistudio.mylauncher.abcd

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import kotlin.math.abs

class Workspace @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val scroller = OverScroller(context)
    private val touchSlop: Int
    private var isBeingDragged = false
    private var lastMotionX = 0f
    private var lastMotionY = 0f
    private var initialMotionX = 0f

    private var currentPageIndex = 0
    var onPageChanged: ((Int) -> Unit)? = null

    init {
        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        for (i in 0 until childCount) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var childLeft = 0
        val childWidth = r - l
        val childHeight = b - t

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.layout(childLeft, 0, childLeft + childWidth, childHeight)
                childLeft += childWidth
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked

        // Always handle cancel and up
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isBeingDragged = false
            return false
        }

        // If dragging, intercept immediately
        if (action != MotionEvent.ACTION_DOWN && isBeingDragged) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastMotionX = ev.x
                lastMotionY = ev.y
                initialMotionX = ev.x
                
                // If scrolling was happening, stop it and start dragging
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                    isBeingDragged = true
                } else {
                    isBeingDragged = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.x
                val y = ev.y
                val xDiff = abs(x - lastMotionX)
                val yDiff = abs(y - lastMotionY)

                if (xDiff > touchSlop && xDiff > yDiff * 1.5f) {
                    isBeingDragged = true
                    lastMotionX = x
                    lastMotionY = y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }

        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isBeingDragged && ev.actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastMotionX = ev.x
                lastMotionY = ev.y
                initialMotionX = ev.x
                isBeingDragged = true
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.x
                val deltaX = lastMotionX - x
                lastMotionX = x

                scrollBy(deltaX.toInt(), 0)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    isBeingDragged = false
                    snapToDestination()
                }
            }
        }
        return true
    }

    private fun snapToDestination() {
        val screenWidth = width
        val scrollX = scrollX
        var targetPage = (scrollX + screenWidth / 2) / screenWidth
        targetPage = targetPage.coerceIn(0, (childCount - 1).coerceAtLeast(0))
        snapToPage(targetPage)
    }

    private fun snapToPage(page: Int) {
        val newPage = page.coerceIn(0, (childCount - 1).coerceAtLeast(0))
        val delta = (newPage * width) - scrollX
        scroller.startScroll(scrollX, 0, delta, 0, 300)
        invalidate()
        
        if (currentPageIndex != newPage) {
            currentPageIndex = newPage
            AppLogger.d("Workspace", "Page changed to $currentPageIndex")
            onPageChanged?.invoke(currentPageIndex)
        }
    }

    fun setCurrentPage(index: Int, animate: Boolean) {
        val targetPage = index.coerceIn(0, (childCount - 1).coerceAtLeast(0))
        if (targetPage != currentPageIndex) {
            currentPageIndex = targetPage
            if (animate) {
                snapToPage(targetPage)
            } else {
                scrollTo(targetPage * width, 0)
                onPageChanged?.invoke(currentPageIndex)
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }
}
