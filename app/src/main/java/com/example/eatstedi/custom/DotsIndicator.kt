package com.example.eatstedi.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.eatstedi.R

class DotsIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val dots = mutableListOf<View>()
    private var dotSize = 16 // default size in dp
    private var dotSpacing = 8 // default spacing in dp
    private var selectedDotColor = Color.parseColor("#FF4081") // default selected color
    private var unselectedDotColor = Color.parseColor("#CCCCCC") // default unselected color

    init {
        orientation = HORIZONTAL

        // Try to get primary color from resources
        try {
            selectedDotColor = ContextCompat.getColor(context, R.color.primary)
        } catch (e: Exception) {
            // Use default if color not found
        }
    }

    fun setDotCount(count: Int) {
        removeAllViews()
        dots.clear()

        for (i in 0 until count) {
            val dot = DotView(context)
            val params = LayoutParams(
                dotSize.dpToPx(),
                dotSize.dpToPx()
            )
            params.setMargins(dotSpacing.dpToPx(), 0, dotSpacing.dpToPx(), 0)
            dot.layoutParams = params

            dots.add(dot)
            addView(dot)
        }

        setSelectedDot(0)
    }

    fun setSelectedDot(position: Int) {
        if (position < 0 || position >= dots.size) return

        dots.forEachIndexed { index, dot ->
            (dot as DotView).setSelected(index == position)
        }
    }

    private inner class DotView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var isSelected = false

        init {
            paint.color = unselectedDotColor
            paint.style = Paint.Style.FILL
        }

        override fun setSelected(selected: Boolean) {
            isSelected = selected
            paint.color = if (selected) selectedDotColor else unselectedDotColor
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = Math.min(width, height) / 2f
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }

    private fun Int.dpToPx(): Int {
        val scale = context.resources.displayMetrics.density
        return (this * scale).toInt()
    }
}