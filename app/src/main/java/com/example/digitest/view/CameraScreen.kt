package com.example.digitest.view

import android.app.Activity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitest.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val activity = context as? Activity
    val lifecycleOwner = activity as? LifecycleOwner ?: return

    LaunchedEffect(Unit) {
        if (!viewModel.isCaptureCompleted.value) {
            viewModel.startCamera(previewView, lifecycleOwner) { detected, face, bitmap ->
                viewModel.faceDetectedState.value = detected
                face?.let {
                    if (bitmap != null) {
                        viewModel.processFaceAlignment(it, bitmap)
                    }
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2196F3))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(
                        if (viewModel.faceDetectedState.value) Color.Green else Color.Red,
                        CircleShape
                    )
            )
        }

        Text(
            text = viewModel.instructionText.value,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AlignmentOption.values().forEach { alignment ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (viewModel.capturedStates.value[alignment] == true) Color.Green else Color.LightGray,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.capturedImages.value[alignment] != null) {
                        Image(
                            bitmap = viewModel.capturedImages.value[alignment]!!.asImageBitmap(),
                            contentDescription = "Captured Face for ${alignment.name}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Person, contentDescription = alignment.name)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = alignment.name,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
        ) {
            Text(
                text = "Submit",
                color = Color.DarkGray
            )
        }
    }
}
