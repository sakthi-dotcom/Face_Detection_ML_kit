package com.example.digitest.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.example.digitest.model.CameraRepository
import com.example.digitest.model.IoExecutor
import com.example.digitest.view.AlignmentOption
import com.google.mlkit.vision.face.Face
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import javax.inject.Inject


//Handles the business logic and state management.
@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val cameraRepository: CameraRepository,
    @IoExecutor private val cameraExecutor: ExecutorService
) : AndroidViewModel(application) {

    private val _faceDetectedState = MutableStateFlow(false)
    val isCaptureCompleted = mutableStateOf(false)
    private val alignmentState = mutableStateOf(AlignmentOption.Center)
    val capturedStates = mutableStateOf(
        mapOf(AlignmentOption.Center to false, AlignmentOption.Left to false, AlignmentOption.Right to false)
    )
    val instructionText = mutableStateOf("Keep your head straight")
    val faceDetectedState = mutableStateOf(false)
    private val _capturedImages = MutableStateFlow(
        AlignmentOption.values().associateWith { null as Bitmap? }
    )
    val capturedImages: StateFlow<Map<AlignmentOption, Bitmap?>> get() = _capturedImages

    fun processFaceAlignment(face: Face,capturedFace: Bitmap,rotationDegrees: Int) {
        val yaw = face.headEulerAngleY
        when {
            alignmentState.value == AlignmentOption.Center && yaw in -15f..15f -> {
                captureFace(AlignmentOption.Center,capturedFace,rotationDegrees)
                alignmentState.value = AlignmentOption.Left
                instructionText.value = "Turn your face to the left"
            }
            alignmentState.value == AlignmentOption.Left && yaw < -15f -> {
                captureFace(AlignmentOption.Left,capturedFace,rotationDegrees)
                alignmentState.value = AlignmentOption.Right
                instructionText.value = "Turn your face to the right"
            }
            alignmentState.value == AlignmentOption.Right && yaw > 15f -> {
                captureFace(AlignmentOption.Right,capturedFace,rotationDegrees)
                isCaptureCompleted.value = true
                instructionText.value = "Face capture complete"
            }
        }
    }

    private fun captureFace(alignment: AlignmentOption, capturedFace: Bitmap?,rotationDegrees:Int) {
        capturedFace?.let {
            val rotatedBitmap = rotateBitmap(it, rotationDegrees)
            val circularBitmap = createCircularBitmap(rotatedBitmap)
            _capturedImages.value = _capturedImages.value.toMutableMap().apply {
                this[alignment] = circularBitmap
            }
            capturedStates.value = capturedStates.value.toMutableMap().apply {
                this[alignment] = true
            }
        }
    }

    private fun createCircularBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2).toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }


    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onFaceDetected: (Boolean, Face?, Bitmap?,Int) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { image ->
                    cameraRepository.processImage(image) { detected, face, bitmap ->
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        onFaceDetected(detected, face, bitmap,rotationDegrees)
                    }
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}
