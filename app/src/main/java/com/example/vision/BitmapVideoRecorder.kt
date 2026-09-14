package com.example.vision

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BitmapVideoRecorder"

/**
 * Fallback hardware-accelerated MP4 video recorder that encodes Bitmaps
 * directly using MediaCodec and MediaMuxer.
 * Works seamlessly on any Android device or emulator even when CameraX cannot
 * bind 3 simultaneous use cases.
 */
class BitmapVideoRecorder(
    private val outputFile: File,
    private val width: Int = 640,
    private val height: Int = 640,
    private val frameRate: Int = 30
) {
    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val isRecording = AtomicBoolean(false)
    private var frameCount = 0L
    private val bufferInfo = MediaCodec.BufferInfo()
    private val destRect = Rect(0, 0, width, height)

    @Synchronized
    fun start(): Boolean {
        if (isRecording.get()) return true
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            codec = encoder

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerStarted = false
            trackIndex = -1
            frameCount = 0L
            isRecording.set(true)
            Log.i(TAG, "BitmapVideoRecorder started -> ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BitmapVideoRecorder: ${e.message}", e)
            release()
            return false
        }
    }

    @Synchronized
    fun recordFrame(bitmap: Bitmap) {
        if (!isRecording.get()) return
        val surface = inputSurface ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }
            canvas.drawBitmap(bitmap, null, destRect, null)
            surface.unlockCanvasAndPost(canvas)
            frameCount++
            drainEncoder(endOfStream = false)
        } catch (e: Exception) {
            Log.w(TAG, "Error recording frame to MP4: ${e.message}")
        }
    }

    @Synchronized
    fun stop(): File? {
        if (!isRecording.getAndSet(false)) return outputFile
        try {
            codec?.signalEndOfInputStream()
            drainEncoder(endOfStream = true)
        } catch (e: Exception) {
            Log.w(TAG, "Error signaling end of stream: ${e.message}")
        } finally {
            release()
        }
        return if (outputFile.exists() && outputFile.length() > 0) {
            Log.i(TAG, "BitmapVideoRecorder completed: ${outputFile.length()} bytes, $frameCount frames")
            outputFile
        } else {
            Log.w(TAG, "BitmapVideoRecorder produced empty or missing file")
            null
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val encoder = codec ?: return
        val mux = muxer ?: return

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) {
                        Log.e(TAG, "Format changed after muxer started")
                        break
                    }
                    val newFormat = encoder.outputFormat
                    trackIndex = mux.addTrack(newFormat)
                    mux.start()
                    muxerStarted = true
                    Log.d(TAG, "Muxer started with trackIndex $trackIndex")
                }
                outIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mux.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    private fun release() {
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null

        try {
            inputSurface?.release()
        } catch (_: Exception) {}
        inputSurface = null

        try {
            if (muxerStarted) {
                muxer?.stop()
            }
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
    }
}
