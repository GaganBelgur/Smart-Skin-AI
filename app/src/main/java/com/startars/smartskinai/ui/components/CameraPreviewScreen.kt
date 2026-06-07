package com.startars.smartskinai.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier, imageCapture: ImageCapture) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraManager = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            cameraManager.addListener({
                val cameraProvider = cameraManager.get()
                val preview = Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreviewScreen", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}