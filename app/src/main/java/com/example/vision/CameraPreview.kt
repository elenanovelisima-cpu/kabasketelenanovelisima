package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    onFrameAnalyzed: (Bitmap) -> Unit,
    onCameraStateChanged: (Boolean) -> Unit,
    useFrontCamera: Boolean = false,
    isRecording: Boolean = false,
    onVideoRecorded: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val previewRef = remember { mutableStateOf<Preview?>(null) }
    val imageAnalysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }
    val bitmapRecorderRef = remember { mutableStateOf<BitmapVideoRecorder?>(null) }

    fun getDisplayRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        } else {
            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    suspend fun switchCamera(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
        frontCamera: Boolean
    ) {
        withContext(Dispatchers.Main) {
            // 1. Detener el analizador anterior para liberar buffers de imagen antes de desvincular
            try {
                imageAnalysisRef.value?.clearAnalyzer()
                imageAnalysisRef.value = null
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing analyzer: ${e.message}")
            }
            try {
                previewRef.value?.setSurfaceProvider(null)
                previewRef.value = null
            } catch (_: Exception) {}

            // 2. Desvincular use cases previos
            try {
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding camera: ${e.message}")
            }

            // 3. Breve pausa asíncrona para que la capa HAL de Camera2 en Android libere el sensor previo (evita CAMERA_IN_USE)
            delay(120)

            // 4. Determinar sensores disponibles en el dispositivo
            val hasFront = try {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            } catch (_: Exception) {
                false
            }
            val hasBack = try {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            } catch (_: Exception) {
                false
            }

            val cameraSelector = when {
                frontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                !frontCamera && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> try {
                    CameraSelector.Builder().addCameraFilter { it.take(1) }.build()
                } catch (_: Exception) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            }

            val currentDisplayRotation = getDisplayRotation()

            // 5. Configurar nuevo Preview con resolución estándar 4:3 compatible en todos los teléfonos
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(currentDisplayRotation)
                .build()

            // Asignar el surface provider tras el unbindAll
            preview.setSurfaceProvider(previewView.surfaceProvider)
            previewRef.value = preview

            // 6. Configurar ImageAnalysis con resolución 640x480 (4:3) para alineación perfecta con Preview
            val isFront = frontCamera
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setTargetRotation(currentDisplayRotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val rawBitmap = imageProxy.toBitmap()
                    val correctedBitmap = if (rotation != 0 || isFront) {
                        val matrix = android.graphics.Matrix()
                        if (rotation != 0) {
                            matrix.postRotate(rotation.toFloat())
                        }
                        if (isFront) {
                            // Modo espejo horizontal para verse natural en pantalla como un espejo
                            matrix.postScale(-1f, 1f)
                        }
                        val rotated = Bitmap.createBitmap(
                            rawBitmap,
                            0,
                            0,
                            rawBitmap.width,
                            rawBitmap.height,
                            matrix,
                            true
                        )
                        rawBitmap.recycle()
                        rotated
                    } else {
                        rawBitmap
                    }

                    // Enviar frame al grabador si está activo
                    bitmapRecorderRef.value?.recordFrame(correctedBitmap)

                    onFrameAnalyzed(correctedBitmap)
                    correctedBitmap.recycle()
                } catch (e: Exception) {
                    Log.w(TAG, "Frame analysis skipped: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }
            imageAnalysisRef.value = imageAnalysis

            // 7. Bucle de vinculación con hasta 3 reintentos si el hardware de la cámara aún estaba ocupado
            var bound = false
            var attempts = 0
            while (!bound && attempts < 3) {
                attempts++
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    bound = true
                    onCameraStateChanged(true)
                    Log.i(TAG, "Camera bound successfully on attempt $attempts (front=$frontCamera, rotation=$currentDisplayRotation)")
                } catch (e: Exception) {
                    Log.w(TAG, "Camera bind attempt $attempts failed (${e.message}), retrying...")
                    delay(150)
                }
            }

            if (!bound) {
                // Fallback de emergencia: vincular solo la vista previa para no dejar pantalla negra
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    onCameraStateChanged(true)
                    Log.i(TAG, "Camera bound with preview-only fallback")
                } catch (eFatal: Exception) {
                    Log.e(TAG, "Fatal camera binding error: ${eFatal.message}", eFatal)
                    onCameraStateChanged(false)
                }
            }
        }
    }

    // Adaptar rotación de cámara al girar el dispositivo en horizontal
    val orientationEventListener = remember {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                try {
                    previewRef.value?.targetRotation = rotation
                    imageAnalysisRef.value?.targetRotation = rotation
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating target rotation: ${e.message}")
                }
            }
        }
    }

    // Actualizar rotación ante cambios de configuración de pantalla (giro vertical <-> horizontal)
    LaunchedEffect(configuration.orientation) {
        val provider = cameraProviderRef.value
        val previewView = previewViewRef.value
        if (provider != null && previewView != null) {
            switchCamera(provider, previewView, useFrontCamera)
        }
    }

    // Reaccionar inmediatamente ante cambios en useFrontCamera de forma asíncrona y segura
    LaunchedEffect(useFrontCamera) {
        val provider = cameraProviderRef.value
        val previewView = previewViewRef.value
        if (provider != null && previewView != null) {
            switchCamera(provider, previewView, useFrontCamera)
        }
    }

    // Manejo de Inicio / Fin de Grabación de vídeo
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val recFile = File(context.cacheDir, "live_rec_${System.currentTimeMillis()}.mp4")
            val bRecorder = BitmapVideoRecorder(recFile)
            if (bRecorder.start()) {
                bitmapRecorderRef.value = bRecorder
                Log.i(TAG, "Video recorder started: ${recFile.name}")
            }
        } else {
            bitmapRecorderRef.value?.let { bRecorder ->
                try {
                    val savedFile = bRecorder.stop()
                    if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
                        Log.i(TAG, "Video recorder produced file: ${savedFile.absolutePath}, ${savedFile.length()} bytes")
                        onVideoRecorded(Uri.fromFile(savedFile))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recorder: ${e.message}")
                }
                bitmapRecorderRef.value = null
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
            try {
                bitmapRecorderRef.value?.stop()
                bitmapRecorderRef.value = null
            } catch (_: Exception) {}
            try {
                imageAnalysisRef.value?.clearAnalyzer()
                imageAnalysisRef.value = null
            } catch (_: Exception) {}
            try {
                previewRef.value?.setSurfaceProvider(null)
                previewRef.value = null
            } catch (_: Exception) {}
            try {
                cameraProviderRef.value?.unbindAll()
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding camera on dispose: ${e.message}")
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down executor: ${e.message}")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef.value = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef.value = cameraProvider
                        coroutineScope.launch {
                            switchCamera(cameraProvider, previewView, useFrontCamera)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera provider error: ${e.message}", e)
                        onCameraStateChanged(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { pView ->
                previewViewRef.value = pView
            }
        )
    }
}
