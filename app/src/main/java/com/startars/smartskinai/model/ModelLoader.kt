package com.startars.smartskinai.model

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class ModelLoader(
    private val context: Context,
    private val modelFileName: String,
    private val labelFileName: String,
) {

    init {
        if (modelFileName.isBlank()) {
            throw IllegalArgumentException("Model file name cannot be blank")
        }
        if (labelFileName.isBlank()) {
            throw IllegalArgumentException("Label file name cannot be blank")
        }
    }

    fun loadModel(): ByteBuffer {
        val assetManager = context.assets
        val modelFileDescriptor = assetManager.openFd(modelFileName)

        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val channel = inputStream.channel

        val startOffset = modelFileDescriptor.startOffset
        val declaredLength = modelFileDescriptor.declaredLength

        val byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        return byteBuffer
    }

    fun loadLabels(): List<String> {
        return context.assets
            .open(labelFileName)
            .bufferedReader()
            .readLines()
    }

}