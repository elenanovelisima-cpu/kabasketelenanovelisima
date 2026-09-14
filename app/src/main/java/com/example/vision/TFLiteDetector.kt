package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteDetector(private val context: Context) {
    companion object {
        private const val TAG = "TFLiteDetector"
        private const val MODEL_PATH = "models/best_float32.tflite"
        const val INPUT_SIZE = 640
        private const val NUM_ANCHORS = 8400
        private const val BALL_IDX = 1
        private const val HOOP_IDX = 2
        private const val PLAYER_IDX = 3

        private const val BALL_CONF_THRESHOLD = 0.20f
        private const val HOOP_CONF_THRESHOLD = 0.30f
        private const val PLAYER_CONF_THRESHOLD = 0.40f

        // Precomputed lookup table for fast 0..255 byte-to-float normalization
        private val NORM_LUT = FloatArray(256) { it / 255.0f }
    }

    private var interpreter: Interpreter? = null
    var isModelLoaded: Boolean = false
        private set

    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val floatBuffer = inputBuffer.asFloatBuffer()
    private val floatValues = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)

    // Output buffer for YOLOv8: [1, 8, 8400]
    private val outputBuffer = Array(1) { Array(8) { FloatArray(NUM_ANCHORS) } }
    private val pixelValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(MODEL_PATH)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            val options = Interpreter.Options().apply {
                setNumThreads(numCores)
                setUseXNNPACK(true)
                setUseNNAPI(false)
            }
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            Log.i(TAG, "TFLite model successfully loaded from assets: $MODEL_PATH with $numCores threads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}", e)
            isModelLoaded = false
        }
    }

    @Synchronized
    fun detect(bitmap: Bitmap): DetectionFrame {
        val currentInterpreter = interpreter ?: return DetectionFrame(null, null, null)

        val scaledBitmap = if (bitmap.width != INPUT_SIZE || bitmap.height != INPUT_SIZE) {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        } else {
            bitmap
        }

        scaledBitmap.getPixels(pixelValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }

        // Fast bulk RGB normalization using precomputed LUT (Interleaved NHWC)
        var idx = 0
        val lut = NORM_LUT
        val totalPixels = INPUT_SIZE * INPUT_SIZE
        for (i in 0 until totalPixels) {
            val pixel = pixelValues[i]
            floatValues[idx++] = lut[(pixel shr 16) and 0xFF]
            floatValues[idx++] = lut[(pixel shr 8) and 0xFF]
            floatValues[idx++] = lut[pixel and 0xFF]
        }
        floatBuffer.rewind()
        floatBuffer.put(floatValues)

        try {
            currentInterpreter.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Error running inference: ${e.message}", e)
            return DetectionFrame(null, null, null)
        }

        var ball: Det? = null
        var hoop: Det? = null
        var player: Det? = null
        var bestBall = BALL_CONF_THRESHOLD
        var bestHoop = HOOP_CONF_THRESHOLD
        var bestPlayer = PLAYER_CONF_THRESHOLD

        val outMatrix = outputBuffer[0]
        val xs = outMatrix[0]
        val ys = outMatrix[1]
        val ws = outMatrix[2]
        val hs = outMatrix[3]
        val ballScores = outMatrix[4 + BALL_IDX]
        val hoopScores = outMatrix[4 + HOOP_IDX]
        val playerScores = outMatrix[4 + PLAYER_IDX]

        for (i in 0 until NUM_ANCHORS) {
            val bc = ballScores[i]
            val hc = hoopScores[i]
            val pc = playerScores[i]

            if (bc > bestBall || hc > bestHoop || pc > bestPlayer) {
                val x = xs[i] * INPUT_SIZE
                val y = ys[i] * INPUT_SIZE
                val w = ws[i] * INPUT_SIZE
                val h = hs[i] * INPUT_SIZE

                if (bc > bestBall) {
                    bestBall = bc
                    ball = Det(cx = x, cy = y, w = w, h = h, conf = bc)
                }
                if (hc > bestHoop) {
                    bestHoop = hc
                    hoop = Det(cx = x, cy = y, w = w, h = h, conf = hc)
                }
                if (pc > bestPlayer) {
                    bestPlayer = pc
                    player = Det(cx = x, cy = y, w = w, h = h, conf = pc)
                }
            }
        }

        return DetectionFrame(ball = ball, hoop = hoop, player = player)
    }

    @Synchronized
    fun detectAll(bitmap: Bitmap): TacticalDetectionFrame {
        val currentInterpreter = interpreter ?: return TacticalDetectionFrame(null, null, null, emptyList())

        val scaledBitmap = if (bitmap.width != INPUT_SIZE || bitmap.height != INPUT_SIZE) {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        } else {
            bitmap
        }

        scaledBitmap.getPixels(pixelValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }

        var idx = 0
        val lut = NORM_LUT
        val totalPixels = INPUT_SIZE * INPUT_SIZE
        for (i in 0 until totalPixels) {
            val pixel = pixelValues[i]
            floatValues[idx++] = lut[(pixel shr 16) and 0xFF]
            floatValues[idx++] = lut[(pixel shr 8) and 0xFF]
            floatValues[idx++] = lut[pixel and 0xFF]
        }
        floatBuffer.rewind()
        floatBuffer.put(floatValues)

        try {
            currentInterpreter.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Error running multi inference: ${e.message}", e)
            return TacticalDetectionFrame(null, null, null, emptyList())
        }

        var ball: Det? = null
        var hoop: Det? = null
        var bestBall = BALL_CONF_THRESHOLD
        var bestHoop = HOOP_CONF_THRESHOLD

        val rawPlayerCandidates = ArrayList<Det>(64)
        val outMatrix = outputBuffer[0]
        val xs = outMatrix[0]
        val ys = outMatrix[1]
        val ws = outMatrix[2]
        val hs = outMatrix[3]
        val ballScores = outMatrix[4 + BALL_IDX]
        val hoopScores = outMatrix[4 + HOOP_IDX]
        val playerScores = outMatrix[4 + PLAYER_IDX]

        for (i in 0 until NUM_ANCHORS) {
            val bc = ballScores[i]
            val hc = hoopScores[i]
            val pc = playerScores[i]

            if (bc > bestBall || hc > bestHoop || pc > PLAYER_CONF_THRESHOLD) {
                val x = xs[i] * INPUT_SIZE
                val y = ys[i] * INPUT_SIZE
                val w = ws[i] * INPUT_SIZE
                val h = hs[i] * INPUT_SIZE

                if (bc > bestBall) {
                    bestBall = bc
                    ball = Det(cx = x, cy = y, w = w, h = h, conf = bc)
                }
                if (hc > bestHoop) {
                    bestHoop = hc
                    hoop = Det(cx = x, cy = y, w = w, h = h, conf = hc)
                }
                if (pc > PLAYER_CONF_THRESHOLD && w > 15f && h > 25f) {
                    rawPlayerCandidates.add(Det(cx = x, cy = y, w = w, h = h, conf = pc))
                }
            }
        }

        // Fast NMS (Non-Maximum Suppression) for players
        val filteredPlayers = applyNms(rawPlayerCandidates, iouThreshold = 0.45f, maxDetections = 12)
        val bestPlayer = filteredPlayers.firstOrNull()

        return TacticalDetectionFrame(
            ball = ball,
            hoop = hoop,
            player = bestPlayer,
            allPlayers = filteredPlayers
        )
    }

    private fun applyNms(candidates: List<Det>, iouThreshold: Float, maxDetections: Int): List<Det> {
        if (candidates.isEmpty()) return emptyList()
        val sorted = candidates.sortedByDescending { it.conf }
        val selected = ArrayList<Det>(maxDetections)

        for (cand in sorted) {
            var suppress = false
            for (prev in selected) {
                if (calculateIoU(cand, prev) > iouThreshold) {
                    suppress = true
                    break
                }
            }
            if (!suppress) {
                selected.add(cand)
                if (selected.size >= maxDetections) break
            }
        }
        return selected
    }

    private fun calculateIoU(a: Det, b: Det): Float {
        val aLeft = a.cx - a.w / 2f
        val aRight = a.cx + a.w / 2f
        val aTop = a.cy - a.h / 2f
        val aBottom = a.cy + a.h / 2f

        val bLeft = b.cx - b.w / 2f
        val bRight = b.cx + b.w / 2f
        val bTop = b.cy - b.h / 2f
        val bBottom = b.cy + b.h / 2f

        val interLeft = maxOf(aLeft, bLeft)
        val interTop = maxOf(aTop, bTop)
        val interRight = minOf(aRight, bRight)
        val interBottom = minOf(aBottom, bBottom)

        val interWidth = maxOf(0f, interRight - interLeft)
        val interHeight = maxOf(0f, interBottom - interTop)
        val interArea = interWidth * interHeight

        val areaA = a.w * a.h
        val areaB = b.w * b.h
        val unionArea = areaA + areaB - interArea
        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter: ${e.message}", e)
        }
    }
}
