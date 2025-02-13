package com.example.lane_detection

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PipelineViewModel : ViewModel() {
    private val _pipelineImages = MutableLiveData<List<Pair<Bitmap, String>>>()
    val pipelineImages: LiveData<List<Pair<Bitmap, String>>> get() = _pipelineImages

    fun updatePipelineImages(images: List<Pair<Bitmap, String>>) {
        _pipelineImages.value = images
    }
}