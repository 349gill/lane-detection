import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PipelineViewModel : ViewModel() {
    private val _pipelineImages = MutableLiveData<List<Bitmap>>()
    val pipelineImages: LiveData<List<Bitmap>> get() = _pipelineImages

    fun updatePipelineImages(images: List<Bitmap>) {
        _pipelineImages.value = images
    }
}