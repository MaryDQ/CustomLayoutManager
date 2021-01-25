package com.mlx.customlayoutmanager

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

class FlowLayoutManager : RecyclerView.LayoutManager() {
    private var mPendingPosition = RecyclerView.NO_POSITION

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (null == recycler || null == state) {
            return

        }
        val totalSpace = width - paddingLeft - paddingRight
        var availableSpace = if (heightMode == View.MeasureSpec.AT_MOST) {
            Int.MAX_VALUE
        } else {
            height - paddingTop - paddingBottom
        }
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
        var tempMaxBottom = 0
        var shouldBreak = false

        while (currentPosition < state.itemCount) {
            val view = recycler.getViewForPosition(currentPosition)
            addView(view)

            measureChild(view, 0, 0)

            if (left + getDecoratedMeasuredWidth(view) > totalSpace) {
                if (shouldBreak) {
                    removeAndRecycleView(view,recycler)
                    break
                }
                left = 0
                top = tempMaxBottom
                if (!shouldBreak) {
                    availableSpace -= tempMaxBottom-getDecoratedMeasuredHeight(view)
                    shouldBreak = availableSpace < 0
                }
            }

            right = left + getDecoratedMeasuredWidth(view)
            bottom = top + getDecoratedMeasuredHeight(view)
            layoutDecorated(view, left, top, right, bottom)
            tempMaxBottom = max(tempMaxBottom, bottom)

            currentPosition++
            left += getDecoratedMeasuredWidth(view)


        }

        offsetChildrenVertical(fixOffset)
    }


    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        //dx>0,代表从右到左滑动
        if (null == recycler || null == state) {
            return 0
        }

        val consumed = fill(dy, recycler)

        offsetChildrenVertical(-consumed)

        recycle(dy,recycler)

        return consumed
    }

    private fun recycle(dx: Int, recycler: RecyclerView.Recycler) {
        val recycledViews = HashSet<View>()
        val finalChildCount = childCount
        if (dx > 0) {
            for (i in 0 until finalChildCount) {
                val view = getChildAt(i)!!
                val bottom = getDecoratedBottom(view)
                if (bottom <= 0) {
                    recycledViews.add(view)
                } else {
                    break
                }
            }
        }

        if (dx < 0) {
            for (i in finalChildCount - 1 downTo 0) {
                val view = getChildAt(i)!!
                val top = getDecoratedTop(view)
                if (top >= height) {
                    recycledViews.add(view)
                } else {
                    break
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

        val totalSpace = width - paddingLeft - paddingRight
        var availableSpace = abs(dx)
        var absDelta = abs(dx)

        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        var tempMaxBottom = 0
        var shouldBreak = false

        if (dx > 0) {
            val anchorView = getChildAt(finalChildCount - 1) ?: return dx
            val anchorPosition = getPosition(anchorView)

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

        if (dx < 0) {
            val anchorView = getChildAt(0) ?: return dx
            val anchorPosition = getPosition(anchorView)

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

        while (availableSpace > 0 && fillPosition in 0 until itemCount) {
            val view = recycler.getViewForPosition(fillPosition)

            if (dx > 0) {
                addView(view)
            } else {
                addView(view, 0)
            }

            measureChild(view, 0, 0)

            if (dx > 0) {
                bottom = top + getDecoratedMeasuredHeight(view)
                if (left + getDecoratedMeasuredWidth(view) > totalSpace) {
                    if (shouldBreak) {
                        break
                    }
                    left = 0
                    top = tempMaxBottom
                    if (!shouldBreak) {
                        availableSpace -= tempMaxBottom-getDecoratedMeasuredHeight(view)
                        shouldBreak = availableSpace < 0
                    }
                }
                tempMaxBottom = bottom
            } else {
                top = bottom - getDecoratedMeasuredHeight(view)
                if (left + getDecoratedMeasuredWidth(view) > totalSpace) {
                    if (shouldBreak) {
                        break
                    }
                    left = 0
                    bottom = tempMaxBottom
                    if (!shouldBreak) {
                        availableSpace -= tempMaxBottom-getDecoratedMeasuredHeight(view)
                        shouldBreak = availableSpace < 0
                    }
                }
                tempMaxBottom = top
            }

            right = left + getDecoratedMeasuredWidth(view)

            layoutDecorated(view, left, top, right, bottom)

            if (dx > 0) {
                left+=getDecoratedMeasuredWidth(view)
                fillPosition++
            } else {
                // TODO: 2021/1/25 判断顺序view添加的顺序(可以采用rect记录下每个item的边界)
                left+=getDecoratedMeasuredWidth(view)
                fillPosition--
            }

        }


        return dx
    }
}