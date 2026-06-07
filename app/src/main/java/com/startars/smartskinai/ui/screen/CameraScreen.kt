package com.startars.smartskinai.ui.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.startars.smartskinai.ui.components.CameraPreviewScreen
import com.startars.smartskinai.utilities.ImageClassifier
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.core.graphics.get

@Composable
fun CameraScreen(modifier: Modifier) {
    val context = LocalContext.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = modifier) {
            CameraPreviewScreen(modifier = modifier, imageCapture)

            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                enabled = !isCapturing,
                onClick = {
                    isCapturing = true
                    val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        options, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                processImage(context, file)
                            }

                            override fun onError(e: ImageCaptureException) {
                                isCapturing = false
                            }
                        })
                }
            ) {
                if (isCapturing) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Capture")
            }
        }
    }
}

private fun processImage(context: Context, file: File) {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

    Log.d("SkinAI", "Width=${bitmap.width}")
    Log.d("SkinAI", "Height=${bitmap.height}")

    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
    val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).order(ByteOrder.nativeOrder())

    for (y in 0 until 224) {
        for (x in 0 until 224) {
            val pixel = resizedBitmap[x, y]

            inputBuffer.putFloat((Color.red(pixel) - 127.5f) / 127.5f)
            inputBuffer.putFloat((Color.green(pixel) - 127.5f) / 127.5f)
            inputBuffer.putFloat((Color.blue(pixel) - 127.5f) / 127.5f)
        }
    }

    inputBuffer.rewind()

    val outputArray = Array(1) { FloatArray(1001) }

    val imageClassifier = ImageClassifier(context)
    val labels = imageClassifier.getLabels()
    with(imageClassifier.getInterpreter()) {
        run(inputBuffer, outputArray)
        getOutputTensor(0).also {
            Log.d("SkinAI", "Output shape: ${it.shape().contentToString()}")
            Log.d("SkinAI", "Output data type: ${it.dataType()}")
        }

        Log.d(
            "SkinAI",
            "Input shape: ${getInputTensor(0).shape().contentToString()}"
        )

        Log.d(
            "SkinAI",
            "Input data type: ${getInputTensor(0).dataType()}"
        )

        val outputShape = getOutputTensor(0).shape()

        Log.d("SkinAI", "Output Shape = ${outputShape.contentToString()}")

        var maxIndex = 0
        var maxScore = outputArray[0][0]

        for (i in outputArray[0].indices) {
            if (outputArray[0][i] > maxScore) {
                maxScore = outputArray[0][i]
                maxIndex = i
            }
        }

        Log.d("SkinAI", "Top Index: $maxIndex")
        Log.d("SkinAI", "Confidence: $maxScore")

        for (i in 0 until 5) {
            Log.d("SkinAI", "Label[$i] = ${labels[i]}")
        }

        val prediction = labels[maxIndex]

        Log.d("SkinAI", "Prediction = $prediction")
        Log.d("SkinAI", "Confidence = ${maxScore * 100}%")
    }
}
