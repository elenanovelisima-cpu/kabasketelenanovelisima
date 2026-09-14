package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Utility to encode bitmap frames into a standard MP4 (H.264 / AVC) video file.
 * Compatible with Android API 21+ using hardware/software MediaCodec and MediaMuxer.
 */
object VideoEncoderHelper {
    private const val TAG = "VideoEncoderHelper"
    private const val MIME_TYPE = "video/avc"
    private const val FRAME_RATE = 15 // 15 fps
    private const val I_FRAME_INTERVAL = 1

    fun encodeFramesToMp4(
        frames: List<Bitmap>,
        outputFile: File,
        targetWidth: Int = 480,
        targetHeight: Int = 480,
        durationSec: Int = 5
    ): Boolean {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            // Ensure even dimensions required by AVC encoder
            val width = if (targetWidth % 2 == 0) targetWidth else targetWidth - 1
            val height = if (targetHeight % 2 == 0) targetHeight else targetHeight - 1

            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 1_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = maxOf(frames.size, (durationSec.coerceAtLeast(2) * FRAME_RATE))

            val workingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(workingBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            var frameIndex = 0
            while (frameIndex < totalFrames) {
                // Select frame
                val sourceBm = if (frames.isNotEmpty()) {
                    frames[frameIndex % frames.size]
                } else null

                if (sourceBm != null && !sourceBm.isRecycled) {
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(sourceBm, width, height, true),
                        0f,
                        0f,
                        paint
                    )
                } else {
                    canvas.drawColor(Color.rgb(18, 20, 32))
                    paint.color = Color.rgb(255, 107, 0)
                    paint.textSize = 28f
                    canvas.drawText("BALL AI SESSION", width / 4f, height / 2f, paint)
                }

                val yuvData = bitmapToYuv420p(workingBitmap, width, height)

                // Feed input buffer
                val inputBufferIndex = encoder.dequeueInputBuffer(10_000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(yuvData)
                        val presentationTimeUs = (frameIndex * 1_000_000L) / FRAME_RATE
                        val isLast = (frameIndex == totalFrames - 1)
                        val flags = if (isLast) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        encoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            yuvData.size,
                            presentationTimeUs,
                            flags
                        )
                        frameIndex++
                    }
                }

                // Drain output buffer
                var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                    outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                }

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    val newFormat = encoder.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }

            // Drain remaining output
            var eos = false
            var drainAttempts = 0
            while (!eos && drainAttempts < 20) {
                drainAttempts++
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 20_000L)
                if (outIndex >= 0) {
                    val outBuf = encoder.getOutputBuffer(outIndex)
                    if (outBuf != null && muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, outBuf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    val newFormat = encoder.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else {
                    break
                }
            }

            workingBitmap.recycle()
            Log.d(TAG, "Successfully encoded MP4 session video: ${outputFile.length()} bytes")
            return outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding session to MP4: ${e.message}", e)
            return false
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun bitmapToYuv420p(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val yuv = ByteArray(width * height * 3 / 2)
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uIndex = width * height
        var vIndex = width * height + (width * height / 4)

        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int

        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[index++]
                r = (color and 0xFF0000) shr 16
                g = (color and 0x00FF00) shr 8
                b = color and 0x0000FF

                y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    if (uIndex < yuv.size) yuv[uIndex++] = u.coerceIn(0, 255).toByte()
                    if (vIndex < yuv.size) yuv[vIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        return yuv
    }
}
