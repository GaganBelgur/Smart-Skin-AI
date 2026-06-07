package com.startars.smartskinai.utilities

import android.content.Context
import com.startars.smartskinai.model.ModelLoader
import org.tensorflow.lite.Interpreter

class ImageClassifier(context: Context) {
    private val interpreter: Interpreter

    private var modelBuffer: ModelLoader = ModelLoader(context, "mobilenet_v1_1.0_224.tflite", "labels.txt")

    init {
        interpreter = Interpreter(modelBuffer.loadModel())
    }

    fun getInterpreter() = interpreter

    fun getLabels() = modelBuffer.loadLabels()
}