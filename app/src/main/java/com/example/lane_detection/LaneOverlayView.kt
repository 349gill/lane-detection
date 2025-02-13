package com.example.lane_detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LaneOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private var laneLines: List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = emptyList()

    fun updateLaneLines(lines: List<Pair<Pair<Float, Float>, Pair<Float, Float>>>) {
        laneLines = lines
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((start, end) in laneLines) {
            canvas.drawLine(start.first, start.second, end.first, end.second, paint)
        }
    }
}
