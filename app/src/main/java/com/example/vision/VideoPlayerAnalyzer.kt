package com.example.vision

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.theme.SportBorder
import com.example.theme.SportOnBrand
import com.example.theme.SportOnSurface
import com.example.theme.SportOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerAnalyzer(
    videoUri: Uri,
    state: VisionState,
    onFrameAnalyzed: (Bitmap) -> Unit,
    onHoopSelected: (Float, Float) -> Unit,
    onConfirmLockHoop: () -> Unit,
    onBackToLiveCamera: () -> Unit,
    onPickAnotherVideo: () -> Unit,
    onOpenSavedVideos: () -> Unit = {},
    onResetStats: () -> Unit,
    debug: Boolean,
    onToggleDebug: () -> Unit,
    onToggleHoopVisual: () -> Unit = {},
    onShowTacticalReport: () -> Unit = {},
    onToggleShowSkeleton: () -> Unit = {},
    onToggleShowHoop: () -> Unit = {},
    onToggleShowBall: () -> Unit = {},
    onCycleHoopPerspective: () -> Unit = {},
    onSetHoopPerspective: (HoopPerspective) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = onToggleShowHoop,
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = onSetHoopPerspective,
            onOpenSavedVideos = onOpenSavedVideos
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var isCalibratingHoopInVideo by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) } // 1.0x normal speed by default
    var showTacticalRadar by remember { mutableStateOf(true) }

    // Find precomputed frame matching exact current video millisecond
    val currentFrame = remember(currentPositionMs, state.videoTimeline) {
        if (state.videoTimeline.isNotEmpty()) {
            findClosestVideoFrame(state.videoTimeline, currentPositionMs.toLong())
        } else null
    }

    val effectiveState = if (currentFrame != null) {
        state.copy(
            ball = currentFrame.ball,
            player = currentFrame.player,
            skeleton = currentFrame.skeleton,
            hoop = currentFrame.hoop ?: state.hoop,
            lockedHoop = state.lockedHoop ?: currentFrame.hoop?.let {
                LockedHoop(nx = it.nx, ny = it.ny, nw = it.nw, nh = it.nh, isLocked = true)
            },
            makes = currentFrame.makes,
            attempts = currentFrame.attempts,
            misses = currentFrame.misses,
            accuracy = currentFrame.accuracy,
            currentStreak = currentFrame.currentStreak,
            lastEvent = currentFrame.lastEvent,
            lastLocation = currentFrame.lastLocation,
            releaseAngle = currentFrame.releaseAngle,
            releaseTimeSec = currentFrame.releaseTimeSec,
            shots = currentFrame.shots,
            courtShots = currentFrame.courtShots,
            currentTacticalAnalysis = currentFrame.tacticalAnalysis ?: state.currentTacticalAnalysis
        )
    } else {
        state
    }

    var isAnalyzingFrame by remember { mutableStateOf(false) }

    fun stepFrame(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        mp.pause()
        isPlaying = false
        val newPos = (mp.currentPosition + deltaMs).coerceIn(0, durationMs)
        mp.seekTo(newPos)
        currentPositionMs = newPos
        if (state.videoTimeline.isEmpty()) {
            coroutineScope.launch(Dispatchers.Default) {
                delay(35)
                val tv = textureViewRef
                if (tv != null) {
                    val frame = tv.getBitmap(640, 640)
                    if (frame != null) {
                        onFrameAnalyzed(frame)
                    }
                }
            }
        }
    }

    // High-frequency, synchronized frame extraction loop or 60 FPS timeline tracker
    LaunchedEffect(isPlaying, mediaPlayer, state.videoTimeline) {
        val mp = mediaPlayer
        if (mp != null) {
            val hasTimeline = state.videoTimeline.isNotEmpty()
            while (isActive && isPlaying) {
                try {
                    currentPositionMs = mp.currentPosition
                    durationMs = maxOf(1, mp.duration)

                    if (!hasTimeline) {
                        if (!isAnalyzingFrame) {
                            val tv = textureViewRef
                            if (tv != null) {
                                val frameBitmap = tv.getBitmap(640, 640)
                                if (frameBitmap != null) {
                                    isAnalyzingFrame = true
                                    coroutineScope.launch(Dispatchers.Default) {
                                        try {
                                            onFrameAnalyzed(frameBitmap)
                                        } finally {
                                            isAnalyzingFrame = false
                                        }
                                    }
                                }
                            }
                        }
                        delay(60)
                    } else {
                        // Ultra-smooth 60 FPS UI tracking: NO bitmap allocations, NO neural stall, zero lag!
                        delay(16)
                    }
                } catch (e: Exception) {
                    Log.w("VideoPlayerAnalyzer", "Playback tracking error: ${e.message}")
                    delay(30)
                }
            }
        }
    }

    DisposableEffect(videoUri) {
        val mp = MediaPlayer().apply {
            try {
                setDataSource(context, videoUri)
                isLooping = true
                setOnPreparedListener { preparedMp ->
                    durationMs = preparedMp.duration
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            preparedMp.playbackParams = preparedMp.playbackParams.setSpeed(playbackSpeed)
                        } catch (e: Exception) {
                            Log.w("VideoPlayerAnalyzer", "Could not set initial speed: ${e.message}")
                        }
                    }
                    preparedMp.start()
                    isPlaying = true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("VideoPlayerAnalyzer", "Error setting data source: ${e.message}", e)
            }
        }
        mediaPlayer = mp

        onDispose {
            try {
                mp.stop()
                mp.release()
            } catch (e: Exception) {
                Log.e("VideoPlayerAnalyzer", "Error releasing MediaPlayer: ${e.message}")
            }
            mediaPlayer = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_analyzer")
    ) {
        // Video Surface (TextureView)
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isCalibratingHoopInVideo) {
                    if (isCalibratingHoopInVideo) {
                        detectTapGestures { offset ->
                            val nx = offset.x / size.width
                            val ny = offset.y / size.height
                            onHoopSelected(nx, ny)
                        }
                    }
                },
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            textureViewRef = this@apply
                            mediaPlayer?.setSurface(Surface(surface))
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            textureViewRef = null
                            mediaPlayer?.setSurface(null)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            }
        )

        // Computer Vision Overlay: Real-time Skeleton + Frontal Hoop/Tablero + Ball Ring (NO trajectory lines)
        DetectionOverlay(
            state = effectiveState,
            modifier = Modifier.fillMaxSize()
        )

        // HUD Overlay for Video Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Navigation, Speed, Hoop Style, Pick Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back to live camera
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("back_to_camera_button")
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, SportBorder, CircleShape)
                            .clickable { onBackToLiveCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a cámara",
                            tint = SportOnSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (state.activeAnalysisType == AnalysisType.SHOOTING) Color(0xFFFF9800) else Color(0xFF00E5FF))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.activeAnalysisType.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                            Text(
                                text = if (state.activeAnalysisType == AnalysisType.SHOOTING) "ANÁLISIS DE TIROS" else "PARTIDO TÁCTICO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = SportOnSurface
                            )
                        }
                        Text(
                            text = "${state.fps} FPS • Guardado en Mis Vídeos ✓",
                            fontSize = 10.sp,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: Speed selector, Hoop visual toggle, and Lock Hoop
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback speed chips
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(0.5f to "0.5x", 0.75f to "0.75x", 1.0f to "1.0x").forEach { (spd, label) ->
                            val selected = (playbackSpeed == spd)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF00E5FF) else Color.Transparent)
                                    .clickable {
                                        playbackSpeed = spd
                                        val mp = mediaPlayer
                                        if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            try {
                                                mp.playbackParams = mp.playbackParams.setSpeed(spd)
                                            } catch (e: Exception) {
                                                Log.w("VideoPlayerAnalyzer", "Error setting speed: ${e.message}")
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    // Hoop Visual Toggle (Realista vs Solo Tablero)
                    Box(
                        modifier = Modifier
                            .testTag("video_toggle_hoop_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                            .clickable { onToggleHoopVisual() }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.hoopVisual == HoopVisual.REALISTIC_FRONT) "ARO: REALISTA" else "ARO: TABLERO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    // Tactical Match Report button
                    if (state.tacticalReport != null) {
                        Box(
                            modifier = Modifier
                                .testTag("video_tactical_report_button")
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E5FF))
                                .clickable { onShowTacticalReport() }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "INFORME TÁCTICO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }

                    // Lock Hoop button for video
                    Box(
                        modifier = Modifier
                            .testTag("video_hoop_lock_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCalibratingHoopInVideo) Color(0xFF00E5FF) else Color(0x99000000))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                            .clickable {
                                if (isCalibratingHoopInVideo) {
                                    onConfirmLockHoop()
                                    isCalibratingHoopInVideo = false
                                } else {
                                    isCalibratingHoopInVideo = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCalibratingHoopInVideo) "CONFIRMAR" else "FIJAR ARO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCalibratingHoopInVideo) Color.Black else Color(0xFF00E5FF)
                        )
                    }

                    // Mis Vídeos Library button
                    Box(
                        modifier = Modifier
                            .testTag("player_open_saved_videos_button")
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0xFFFF9800), CircleShape)
                            .clickable { onOpenSavedVideos() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Mis Vídeos",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Settings Button for Layer visibility & perspective
                    Box(
                        modifier = Modifier
                            .testTag("player_settings_button")
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x66FFFFFF), CircleShape)
                            .clickable { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes de Capas",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pick another video
                    Box(
                        modifier = Modifier
                            .testTag("pick_another_video_button")
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, SportBorder, CircleShape)
                            .clickable { onPickAnotherVideo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = "Cambiar vídeo",
                            tint = SportOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Calibrating notice banner if active
            if (isCalibratingHoopInVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE00E5FF))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "TOCA EN EL VÍDEO DÓNDE ESTÁ EL ARO Y PULSA 'CONFIRMAR'",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            // BOTTOM CONTROLS & TIMELINE
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Video Timeline Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val posSec = currentPositionMs / 1000
                    val durSec = durationMs / 1000
                    val posText = String.format("%02d:%02d", posSec / 60, posSec % 60)
                    val durText = String.format("%02d:%02d", durSec / 60, durSec % 60)

                    Text(
                        text = posText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportOnSurface
                    )

                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { frac ->
                            val targetMs = (frac * durationMs).toInt()
                            mediaPlayer?.seekTo(targetMs)
                            currentPositionMs = targetMs
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x44FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = durText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x99FFFFFF)
                    )
                }

                // Playback bar + Scoreboard + Court Map
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause / Replay / Frame-by-frame step
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause
                        Box(
                            modifier = Modifier
                                .testTag("video_play_pause_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SportOrange)
                                .clickable {
                                    val mp = mediaPlayer
                                    if (mp != null) {
                                        if (isPlaying) {
                                            mp.pause()
                                            isPlaying = false
                                        } else {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                tint = SportOnBrand,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Frame step backwards (-100ms)
                        Box(
                            modifier = Modifier
                                .testTag("step_backward_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable { stepFrame(-100) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Fotograma anterior",
                                tint = SportOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Frame step forward (+100ms)
                        Box(
                            modifier = Modifier
                                .testTag("step_forward_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable { stepFrame(100) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Fotograma siguiente",
                                tint = SportOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Replay from start
                        Box(
                            modifier = Modifier
                                .testTag("video_replay_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable {
                                    mediaPlayer?.seekTo(0)
                                    mediaPlayer?.start()
                                    isPlaying = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reiniciar vídeo",
                                tint = SportOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset Stats
                        Box(
                            modifier = Modifier
                                .testTag("video_reset_stats_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable { onResetStats() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar tiros",
                                tint = SportOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Debug toggle
                        Box(
                            modifier = Modifier
                                .testTag("video_debug_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (debug) SportOrange else Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable { onToggleDebug() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Debug",
                                tint = if (debug) SportOnBrand else SportOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Center Scoreboard
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${effectiveState.makes} / ${effectiveState.attempts}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = SportOnSurface
                        )
                        Text(
                            text = "${effectiveState.accuracy}% ACIERTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    // Right: Mini Court Shot Map + Tactical Court Radar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (effectiveState.currentTacticalAnalysis != null) {
                            TacticalCourtMap(
                                analysis = effectiveState.currentTacticalAnalysis,
                                modifier = Modifier
                                    .width(115.dp)
                                    .height(75.dp)
                            )
                        }
                        CourtShotMap(shots = effectiveState.courtShots)
                    }
                }
            }
        }
    }
}

private fun findClosestVideoFrame(timeline: List<VideoFrameAnalysis>, timeMs: Long): VideoFrameAnalysis? {
    if (timeline.isEmpty()) return null
    var low = 0
    var high = timeline.size - 1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val midVal = timeline[mid].timestampMs
        if (midVal < timeMs) {
            low = mid + 1
        } else if (midVal > timeMs) {
            high = mid - 1
        } else {
            return timeline[mid]
        }
    }
    val c1 = timeline.getOrNull(high)
    val c2 = timeline.getOrNull(low)
    return when {
        c1 != null && c2 != null -> if (kotlin.math.abs(c1.timestampMs - timeMs) <= kotlin.math.abs(c2.timestampMs - timeMs)) c1 else c2
        c1 != null -> c1
        else -> c2
    }
}
