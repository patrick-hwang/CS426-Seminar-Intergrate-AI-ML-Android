package com.example.imagetotextpathwa

import android.content.Context
import android.content.res.AssetFileDescriptor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ClassificationResult(val label: String, val confidence: Float)

class TransactionClassifier(context: Context) {
    private val interpreter: Interpreter
    private val vocab: Map<String, Int>
    private val labels: List<String>
    private val inputLength: Int

    init {
        // Load model
        interpreter = Interpreter(loadModelFile(context, "transaction_classifier.tflite"))

        // Load vocab
        val vocabJson = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, Int>>() {}.type
        vocab = Gson().fromJson(vocabJson, type)

        // Load labels
        labels = context.assets.open("labels.txt").bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }

        // Get input length from model
        val inputShape = interpreter.getInputTensor(0).shape()
        // Expected shape [1, length]
        inputLength = if (inputShape.size > 1) inputShape[1] else inputShape[0]
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(text: String): ClassificationResult {
        val tokens = tokenize(text)
        val input = IntArray(inputLength) { i ->
            if (i < tokens.size) tokens[i] else 0 // Padding with 0 (Post-padding)
        }

        // Output shape is [1][num_labels]
        val output = Array(1) { FloatArray(labels.size) }
        
        // The model expects a 2D input [1][inputLength]
        val inputReshaped = Array(1) { input }
        
        interpreter.run(inputReshaped, output)

        var maxIndex = 0
        var maxConfidence = -1f
        for (i in output[0].indices) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i]
                maxIndex = i
            }
        }

        // Use 0.35 threshold as in Python notebook
        val resultLabel = if (maxConfidence > 0.35f) {
            if (maxIndex < labels.size) labels[maxIndex] else "Khác"
        } else {
            "Khác"
        }
        
        return ClassificationResult(resultLabel, maxConfidence)
    }

    private fun tokenize(text: String): List<Int> {
        // Normalize to NFC to ensure consistency with vocab.json
        val normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
        
        // Exact regex from Python training notebook
        val vietnameseRegex = Regex("[^a-z0-9àáâãèéêìíòóôõùúăđĩũơưăạảấầẩẫậắằẳẵặẹẻẽềềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ\\s]")
        
        val words = normalizedText.lowercase()
            .replace(vietnameseRegex, " ") // Replace with space like in Python re.sub
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return words.map { word ->
            vocab[word] ?: 1 // ID 1 for [UNK]
        }
    }

    fun close() {
        interpreter.close()
    }
}
