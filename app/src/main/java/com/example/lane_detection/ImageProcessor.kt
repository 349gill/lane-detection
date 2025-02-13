package com.example.lane_detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class ImageProcessor(
    private val onLanesDetected: (List<Pair<Pair<Float, Float>, Pair<Float, Float>>>) -> Unit,
    private val onPipelineImages: (List<Pair<Bitmap, String>>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image)
        val mat = Mat()
        val pipelineImages = mutableListOf<Pair<Bitmap, String>>()

        try {
            Utils.bitmapToMat(bitmap, mat)

            Core.transpose(mat, mat)
            Core.flip(mat, mat, 1)

            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            pipelineImages.add(bitmapFromMat(gray) to "Grayscale")

            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            pipelineImages.add(bitmapFromMat(blurred) to "Gaussian Blur")

            val edges = Mat()
            Imgproc.Canny(blurred, edges, 50.0, 150.0)
            pipelineImages.add(bitmapFromMat(edges) to "Canny Edge Detection")

            val masked = region(edges)
            pipelineImages.add(bitmapFromMat(masked) to "Region Masked")

            val lines = Mat()
            Imgproc.HoughLinesP(masked, lines, 2.0, Math.PI / 180, 100, 40.0, 5.0)

            val detectedLines = processLines(lines, mat)
            onLanesDetected(detectedLines)

            val lanesBitmap = drawLines(mat, detectedLines)
            pipelineImages.add(lanesBitmap to "Lane Lines")
            onPipelineImages(pipelineImages)
        } finally {
            mat.release()
            image.close()
        }
    }

    private fun region(image: Mat): Mat {
        return image
    }

    private fun processLines(lines: Mat, image: Mat): List<Pair<Pair<Float, Float>, Pair<Float, Float>>> {
        val leftLines = mutableListOf<Pair<Double, Double>>()
        val rightLines = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until lines.rows()) {
            val points = lines.get(i, 0)
            val x1 = points[0]
            val y1 = points[1]
            val x2 = points[2]
            val y2 = points[3]
            val slope = (y2 - y1) / (x2 - x1)
            val intercept = y1 - slope * x1

            if (slope < 0) {
                leftLines.add(Pair(slope, intercept))
            } else {
                rightLines.add(Pair(slope, intercept))
            }
        }

        val leftAvg = leftLines.takeIf { it.isNotEmpty() }?.average() ?: return emptyList()
        val rightAvg = rightLines.takeIf { it.isNotEmpty() }?.average() ?: return emptyList()

        val leftLine = makePoints(image, leftAvg)
        val rightLine = makePoints(image, rightAvg)

        return listOf(leftLine, rightLine)
    }

    private fun List<Pair<Double, Double>>.average(): Pair<Double, Double> {
        val slopes = this.map { it.first }
        val intercepts = this.map { it.second }
        return Pair(slopes.average(), intercepts.average())
    }

    private fun makePoints(image: Mat, line: Pair<Double, Double>): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val (slope, intercept) = line
        val y1 = image.rows().toFloat()
        val y2 = (y1 * 3 / 5)
        val x1 = ((y1 - intercept) / slope).toFloat()
        val x2 = ((y2 - intercept) / slope).toFloat()
        return Pair(Pair(x1, y1), Pair(x2, y2))
    }

    private fun drawLines(image: Mat, lines: List<Pair<Pair<Float, Float>, Pair<Float, Float>>>): Bitmap {
        val lineMat = Mat.zeros(image.size(), CvType.CV_8UC3)
        for ((start, end) in lines) {
            Imgproc.line(lineMat, Point(start.first.toDouble(), start.second.toDouble()),
                Point(end.first.toDouble(), end.second.toDouble()), Scalar(255.0, 0.0, 0.0), 10)
        }
        return bitmapFromMat(lineMat)
    }

    private fun bitmapFromMat(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
