package com.example.lane_detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: androidx.camera.view.PreviewView
    private lateinit var laneOverlay: LaneOverlayView
    private lateinit var cameraExecutor: ExecutorService
    private val pipelineImages = mutableListOf<Pair<Bitmap, String>>()

    private val viewModel: PipelineViewModel by viewModels()

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        laneOverlay = findViewById(R.id.laneOverlay)
        cameraExecutor = Executors.newSingleThreadExecutor()

        OpenCVLoader.initDebug()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        else startCamera()

        val pipelineSwitch = findViewById<Switch>(R.id.pipeline)
        val fragmentContainer = findViewById<FragmentContainerView>(R.id.fragment_container)

        pipelineSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                fragmentContainer.visibility = View.VISIBLE
                laneOverlay.visibility = View.GONE
                previewView.visibility = View.GONE

                val fragment = PipelineFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                fragmentContainer.visibility = View.GONE
                laneOverlay.visibility = View.VISIBLE
                previewView.visibility = View.VISIBLE

                supportFragmentManager.popBackStack()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageProcessor(
                        { lines ->
                            runOnUiThread {
                                laneOverlay.updateLaneLines(lines)
                            }
                        },
                        { imagesWithSteps ->
                            runOnUiThread {
                                pipelineImages.clear()
                                pipelineImages.addAll(imagesWithSteps)
                                viewModel.updatePipelineImages(imagesWithSteps)
                            }
                        }
                    ))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}