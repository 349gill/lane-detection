package com.example.lane_detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream


class ImageProcessor(
    private val onLanesDetected: (List<Pair<Pair<Float, Float>, Pair<Float, Float>>>) -> Unit,
    private val onPipelineImages: (List<Bitmap>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image)
        val mat = Mat()
        val pipelineImages = mutableListOf<Bitmap>()

        try {
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
            pipelineImages.add(bitmapFromMat(mat))

            Imgproc.GaussianBlur(mat, mat, org.opencv.core.Size(5.0, 5.0), 0.0)
            pipelineImages.add(bitmapFromMat(mat))

            Imgproc.Canny(mat, mat, 50.0, 150.0)
            pipelineImages.add(bitmapFromMat(mat))

            val lines = Mat()
            Imgproc.HoughLinesP(mat, lines, 1.0, Math.PI / 180, 50, 50.0, 10.0)
            val houghBitmap = bitmapFromMat(mat)
            pipelineImages.add(houghBitmap)

            val detectedLines = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()
            for (i in 0 until lines.rows()) {
                val points = lines.get(i, 0)
                detectedLines.add(
                    Pair(points[0].toFloat(), points[1].toFloat()) to
                            Pair(points[2].toFloat(), points[3].toFloat()))
            }

            onLanesDetected(detectedLines)
            onPipelineImages(pipelineImages)
        } finally {
            mat.release()
            image.close()
        }
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