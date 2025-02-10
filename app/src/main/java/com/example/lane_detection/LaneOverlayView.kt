package com.example.lane_detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LaneOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private var laneLines: List<Pair<Float, Float>> = emptyList()

    fun updateLaneLines(lines: List<Pair<Float, Float>>) {
        laneLines = lines
        postInvalidate()  // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((x, y) in laneLines) {
            canvas.drawLine(x, height.toFloat(), y, 0f, paint)
        }
    }
}
