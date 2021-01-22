package com.mlx.customlayoutmanager

import android.graphics.PointF
import android.util.Log
import android.view.View
import androidx.annotation.IntRange
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.math.abs

class CustomLayoutManager(@IntRange(from = 0, to = 1) val orientation: Int) :
    RecyclerView.LayoutManager(),
    ScrollVectorProvider {
    private var mPendingPosition = RecyclerView.NO_POSITION

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return if (orientationIsHorizontal()) {
            RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
        } else {
            RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (null == recycler || null == state) {
            return
        }

        layoutChunk(recycler, state)
    }

    private fun layoutChunk(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (orientationIsHorizontal()) {
            layoutHorizontal(recycler, state)
        } else {
            layoutVertical(recycler, state)
        }
    }

    /**
     * 竖向布局放置
     */
    private fun layoutVertical(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        var totalSpace = height - paddingTop - paddingBottom
        var currentPosition = 0

        var fixOffset = 0

        if (childCount > 0) {
            currentPosition = getPosition(getChildAt(0)!!)
            fixOffset = getDecoratedTop(getChildAt(0)!!)
        }

        if (mPendingPosition != RecyclerView.NO_POSITION) {
            currentPosition = mPendingPosition
        }

        detachAndScrapAttachedViews(recycler)

        var left = 0
        var right = 0
        var top = 0
        var bottom = 0

        while (totalSpace > 0 && currentPosition < state.itemCount) {
            val view = recycler.getViewForPosition(currentPosition)
            addView(view)

            measureChild(view, 0, 0)
            bottom = top + getDecoratedMeasuredHeight(view)
            right = left + getDecoratedMeasuredWidth(view)

            layoutDecorated(view, left, top, right, bottom)

            currentPosition++
            top += getDecoratedMeasuredHeight(view)
            Log.d("CUSTOMLAYOUTMANAGER", "当前top的值：$top")
            totalSpace -= getDecoratedMeasuredHeight(view)
        }

        offsetChildrenVertical(fixOffset)
    }

    /**
     * 横向布局放置
     */
    private fun layoutHorizontal(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        var totalSpace = width - paddingRight - paddingLeft
        var currentPosition = 0

        var fixOffset = 0

        if (childCount > 0) {
            currentPosition = getPosition(getChildAt(0)!!)
            fixOffset = getDecoratedLeft(getChildAt(0)!!)
        }

        if (mPendingPosition != RecyclerView.NO_POSITION) {
            currentPosition = mPendingPosition
        }

        detachAndScrapAttachedViews(recycler)

        var left = 0
        var right = 0
        var top = 0
        var bottom = 0

        while (totalSpace > 0 && currentPosition < state.itemCount) {
            val view = recycler.getViewForPosition(currentPosition)
            addView(view)
            // TODO: 2021/1/21 widthUsed干嘛用的
            measureChild(view, 0, 0)

            right = left + getDecoratedMeasuredWidth(view)
            bottom = top + getDecoratedMeasuredHeight(view)
            layoutDecorated(view, left, top, right, bottom)

            currentPosition++
            left += getDecoratedMeasuredWidth(view)
            totalSpace -= getDecoratedMeasuredWidth(view)

        }

        offsetChildrenHorizontal(fixOffset)
    }

    override fun canScrollHorizontally(): Boolean {
        return orientation == RecyclerView.HORIZONTAL
    }

    override fun canScrollVertically(): Boolean {
        return orientation == RecyclerView.VERTICAL
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        //dx>0,代表从右到左滑动
        if (null == recycler || null == state) {
            return 0
        }

        val consumed = fill(dx, recycler)

        offsetChildrenHorizontal(-consumed)

        recycle(consumed, recycler)
        return consumed
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        //dx>0,代表从下到上滑动
        if (null == recycler || null == state) {
            return 0
        }

        val consumed = fill(dy, recycler)

        offsetChildrenVertical(-consumed)

        recycle(consumed, recycler)
        return consumed
    }

    private fun recycle(dx: Int, recycler: RecyclerView.Recycler) {
        val recycledViews = HashSet<View>()
        val finalChildCount = childCount
        if (dx > 0) {
            for (i in 0 until finalChildCount) {
                val view = getChildAt(i)!!
                if (orientation == RecyclerView.HORIZONTAL) {
                    val right = getDecoratedRight(view)
                    if (right <= 0) {
                        recycledViews.add(view)
                    } else {
                        break
                    }
                } else {
                    val bottom = getDecoratedBottom(view)
                    if (bottom <= 0) {
                        recycledViews.add(view)
                    } else {
                        break
                    }
                }
            }
        }

        if (dx < 0) {
            for (i in finalChildCount - 1 downTo 0) {
                val view = getChildAt(i)!!
                if (orientation == RecyclerView.HORIZONTAL) {
                    val left = getDecoratedLeft(view)
                    if (left >= width) {
                        recycledViews.add(view)
                    } else {
                        break
                    }
                } else {
                    val top = getDecoratedTop(view)
                    if (top >= height) {
                        recycledViews.add(view)
                    } else {
                        break
                    }
                }
            }
        }

        recycledViews.forEach {
            removeAndRecycleView(it, recycler)
        }
        recycledViews.clear()
    }

    private fun fill(dx: Int, recycler: RecyclerView.Recycler): Int {
        val finalChildCount = childCount
        var fillPosition = RecyclerView.NO_POSITION

        var availableSpace = abs(dx)
        var absDelta = abs(dx)

        var left = 0
        var right = 0
        var top = 0
        var bottom = 0

        if (dx > 0) {
            val anchorView = getChildAt(finalChildCount - 1) ?: return dx
            val anchorPosition = getPosition(anchorView)

            if (orientationIsHorizontal()) {
                val anchorRight = getDecoratedRight(anchorView)
                left = anchorRight
                fillPosition = anchorPosition + 1

                if (fillPosition >= itemCount && anchorRight - absDelta < width) {
                    val fixedScroll = anchorRight - width
                    return fixedScroll
                }

                if (anchorRight - absDelta > width) {
                    return dx
                }
            } else {
                val anchorBottom = getDecoratedBottom(anchorView)
                top = anchorBottom
                fillPosition = anchorPosition + 1

                if (fillPosition >= itemCount && anchorBottom - absDelta < height) {
                    val fixedScroll = anchorBottom - height
                    return fixedScroll
                }

                if (anchorBottom - absDelta > height) {
                    return dx
                }
            }
        }

        if (dx < 0) {
            val anchorView = getChildAt(0) ?: return dx
            val anchorPosition = getPosition(anchorView)

            if (orientationIsHorizontal()) {
                val anchorLeft = getDecoratedLeft(anchorView)
                right = anchorLeft
                fillPosition = anchorPosition - 1

                if (fillPosition < 0 && anchorLeft + absDelta > 0) {
                    return anchorLeft
                }

                if (anchorLeft + absDelta < 0) {
                    return dx
                }
            } else {
                val anchorTop = getDecoratedTop(anchorView)
                bottom = anchorTop
                fillPosition = anchorPosition - 1

                if (fillPosition < 0 && anchorTop + absDelta > 0) {
                    return anchorTop
                }

                if (anchorTop + absDelta < 0) {
                    return dx
                }
            }
        }

        while (availableSpace > 0 && fillPosition in 0 until itemCount) {
            val view = recycler.getViewForPosition(fillPosition)

            if (dx > 0) {
                addView(view)
            } else {
                addView(view, 0)
            }

            measureChild(view, 0, 0)

            if (dx > 0) {
                if (orientationIsHorizontal()) {
                    right = left + getDecoratedMeasuredWidth(view)
                } else {
                    bottom = top + getDecoratedMeasuredHeight(view)
                }
            } else {
                if (orientationIsHorizontal()) {
                    left = right - getDecoratedMeasuredWidth(view)
                } else {
                    top = bottom - getDecoratedMeasuredHeight(view)
                }
            }

            if (orientationIsHorizontal()) {
                bottom = top + getDecoratedMeasuredHeight(view)
            } else {
                right = left + getDecoratedMeasuredWidth(view)
            }

            layoutDecorated(view, left, top, right, bottom)

            if (dx > 0) {
                if (orientationIsHorizontal()) {
                    left = right
                } else {
                    top = bottom
                }
                fillPosition++
            } else {
                if (orientationIsHorizontal()) {
                    right = left
                } else {
                    bottom = top
                }
                fillPosition--
            }

            if (fillPosition in 0 until itemCount) {
                if (orientationIsHorizontal()) {
                    availableSpace -= getDecoratedMeasuredWidth(view)
                } else {
                    availableSpace -= getDecoratedMeasuredHeight(view)
                }
            }
        }


        return dx
    }

    private fun orientationIsHorizontal(): Boolean {
        return orientation == RecyclerView.HORIZONTAL
    }

    override fun scrollToPosition(position: Int) {
        if (position !in 0 until itemCount) {
            return
        }
        mPendingPosition = position
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val linearSmoothScroller =
            LinearSmoothScroller(recyclerView!!.context)
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        mPendingPosition = RecyclerView.NO_POSITION
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return if (orientationIsHorizontal()) {
            PointF(direction.toFloat(), 0f)
        } else {
            PointF(0f, direction.toFloat())
        }
    }


}