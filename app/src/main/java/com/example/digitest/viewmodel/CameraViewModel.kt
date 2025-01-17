package com.example.digitest.viewmodel

import android.app.Application
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
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val cameraRepository: CameraRepository,
    @IoExecutor private val cameraExecutor: ExecutorService
) : AndroidViewModel(application) {

    val isCaptureCompleted = mutableStateOf(false)
    private val alignmentState = mutableStateOf(AlignmentOption.Center)
    val capturedStates = mutableStateOf(
        mapOf(AlignmentOption.Center to false, AlignmentOption.Left to false, AlignmentOption.Right to false)
    )
    val instructionText = mutableStateOf("Keep your head straight")
    val faceDetectedState = mutableStateOf(false)

    fun processFaceAlignment(face: Face) {
        val yaw = face.headEulerAngleY
        when {
            alignmentState.value == AlignmentOption.Center && yaw in -15f..15f -> {
                captureFace(AlignmentOption.Center)
                alignmentState.value = AlignmentOption.Left
                instructionText.value = "Turn your face to the left"
            }
            alignmentState.value == AlignmentOption.Left && yaw < -15f -> {
                captureFace(AlignmentOption.Left)
                alignmentState.value = AlignmentOption.Right
                instructionText.value = "Turn your face to the right"
            }
            alignmentState.value == AlignmentOption.Right && yaw > 15f -> {
                captureFace(AlignmentOption.Right)
                isCaptureCompleted.value = true
                instructionText.value = "Face capture complete"
            }
        }
    }

    private fun captureFace(alignment: AlignmentOption) {
        capturedStates.value = capturedStates.value.toMutableMap().apply {
            this[alignment] = true
        }
    }

    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onFaceDetected: (Boolean, Face?) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { image ->
                    cameraRepository.processImage(image) { detected, face ->
                        onFaceDetected(detected, face)
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

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}
