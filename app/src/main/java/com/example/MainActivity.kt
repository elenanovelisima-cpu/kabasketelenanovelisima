package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import com.example.home.MainHomeScreen
import com.example.onboarding.OnboardingConfig
import com.example.onboarding.Step4TrainingModeSelectorView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.theme.KaBasketTheme
import com.example.theme.SportBorder
import com.example.theme.SportCanvas
import com.example.theme.SportMuted
import com.example.theme.SportOnBrand
import com.example.theme.SportOnSurface
import com.example.theme.SportOnSurfaceSecondary
import com.example.theme.SportOrange
import com.example.theme.SportWarning
import com.example.vision.AnalysisSpeed
import com.example.vision.AnalysisType
import com.example.vision.CalibrationStep
import com.example.vision.CalibrationWizard
import com.example.vision.CameraPreview
import com.example.vision.DefendZoneHUD
import com.example.vision.DribbleComboHUD
import com.example.vision.InputMode
import com.example.vision.KidsCalibrationStep
import com.example.vision.KidsMiniBasketHUD
import com.example.vision.PlayNowCountdownOverlay
import com.example.vision.ReactionPointsHUD
import com.example.vision.SavedVideosDialog
import com.example.vision.TacticalReportDialog
import com.example.vision.VideoAnalysisCompletedNotice
import com.example.vision.VideoAnalysisNotificationHelper
import com.example.vision.VideoPlayerAnalyzer
import com.example.vision.VideoUploadDialog
import com.example.vision.VisionHUD
import com.example.vision.VisionViewModel

enum class AppScreen {
    HOME,
    WORKOUT_SELECTOR,
    VISION_WORKOUT
}

class MainActivity : ComponentActivity() {

    private val viewModel: VisionViewModel by viewModels()
    private var currentActiveScreen: AppScreen = AppScreen.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mantener la pantalla siempre encendida sin apagarse ni entrar en modo reposo durante entrenamientos y juegos
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        updateSystemBarsForScreen(currentActiveScreen)

        handleIntent(intent)

        setContent {
            KaBasketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SportCanvas
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
                    var initialPickVideo by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(currentScreen) {
                        currentActiveScreen = currentScreen
                        updateSystemBarsForScreen(currentScreen)
                        if (currentScreen != AppScreen.VISION_WORKOUT) {
                            com.example.vision.VoiceCoachManager.stop()
                        }
                    }

                    when (currentScreen) {
                        AppScreen.HOME -> {
                            MainHomeScreen(
                                onNavigateToWorkouts = {
                                    currentScreen = AppScreen.WORKOUT_SELECTOR
                                },
                                onLaunchDefendZoneDrill = {
                                    initialPickVideo = false
                                    viewModel.startDefendZoneMode()
                                    currentScreen = AppScreen.VISION_WORKOUT
                                },
                                onLaunchDribbleDrill = {
                                    initialPickVideo = false
                                    viewModel.startDribbleMode()
                                    currentScreen = AppScreen.VISION_WORKOUT
                                },
                                onLaunchReactionPointsDrill = {
                                    initialPickVideo = false
                                    viewModel.startReactionPointsMode()
                                    currentScreen = AppScreen.VISION_WORKOUT
                                },
                                onLaunchShootingDrill = {
                                    initialPickVideo = false
                                    viewModel.startShootingModeWithTutorial()
                                    currentScreen = AppScreen.VISION_WORKOUT
                                },
                                onLaunchKidsMiniBasketDrill = {
                                    initialPickVideo = false
                                    viewModel.startKidsMiniBasketMode()
                                    currentScreen = AppScreen.VISION_WORKOUT
                                }
                            )
                        }
                        AppScreen.WORKOUT_SELECTOR -> {
                            BackHandler {
                                currentScreen = AppScreen.HOME
                            }
                            Step4TrainingModeSelectorView(
                                onSelectMode = { choice ->
                                    when (choice) {
                                        OnboardingConfig.TrainingModeChoice.KIDS_MINI_BASKET -> {
                                            initialPickVideo = false
                                            viewModel.startKidsMiniBasketMode()
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                        OnboardingConfig.TrainingModeChoice.DEFEND_ZONE -> {
                                            initialPickVideo = false
                                            viewModel.startDefendZoneMode()
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                        OnboardingConfig.TrainingModeChoice.SHOOTING_TUTORIAL -> {
                                            initialPickVideo = false
                                            viewModel.startShootingModeWithTutorial()
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                        OnboardingConfig.TrainingModeChoice.UPLOAD_VIDEO -> {
                                            initialPickVideo = true
                                            viewModel.setCalibrationStep(CalibrationStep.COMPLETED)
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                        OnboardingConfig.TrainingModeChoice.DRIBBLE_CHALLENGE -> {
                                            initialPickVideo = false
                                            viewModel.startDribbleMode()
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                        OnboardingConfig.TrainingModeChoice.DRIBBLE_REACTION_POINTS -> {
                                            initialPickVideo = false
                                            viewModel.startReactionPointsMode()
                                            currentScreen = AppScreen.VISION_WORKOUT
                                        }
                                    }
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }
                        AppScreen.VISION_WORKOUT -> {
                            BackHandler {
                                viewModel.exitToMainMenu()
                                currentScreen = AppScreen.HOME
                            }
                            VisionTestScreen(
                                viewModel = viewModel,
                                onReopenOnboarding = {
                                    viewModel.exitToMainMenu()
                                    currentScreen = AppScreen.HOME
                                },
                                initialPickVideo = initialPickVideo
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(VideoAnalysisNotificationHelper.EXTRA_OPEN_ANALYZED_VIDEO, false) == true) {
            viewModel.openCompletedVideoFromNotice()
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateSystemBarsForScreen(currentActiveScreen)
    }

    override fun onPause() {
        super.onPause()
        com.example.vision.VoiceCoachManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.vision.VoiceCoachManager.shutdown()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateSystemBarsForScreen(currentActiveScreen)
        }
    }

    private fun updateSystemBarsForScreen(screen: AppScreen) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (screen == AppScreen.VISION_WORKOUT) {
            // Durante el entrenamiento / juego de visión en vivo: modo inmersivo completo para máxima área de cámara
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            // Fuera de entrenamientos o minijuegos: mostrar la barra superior (status bar) del móvil
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
            windowInsetsController.isAppearanceLightStatusBars = (screen == AppScreen.HOME)
        }
    }
}

@Composable
fun VisionTestScreen(
    viewModel: VisionViewModel,
    onReopenOnboarding: () -> Unit = {},
    initialPickVideo: Boolean = false
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    var showVideoSourceDialog by remember { mutableStateOf(initialPickVideo) }
    var pendingAnalysisType by remember { mutableStateOf(AnalysisType.SHOOTING) }
    var pendingAnalysisSpeed by remember { mutableStateOf(AnalysisSpeed.TURBO) }
    var pendingRunInBackground by remember { mutableStateOf(false) }
    var pendingEnableSkeleton by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDenied = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 1. Full Document/Storage File Picker (Finds ANY .mp4 in Downloads, WhatsApp, Telegram, Internal Storage, without size limit)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideoUri(
                uri = uri,
                type = pendingAnalysisType,
                speed = pendingAnalysisSpeed,
                runInBackground = pendingRunInBackground,
                enableSkeleton = pendingEnableSkeleton
            )
        }
    }

    // 2. Standard Photo Picker (Finds videos in Gallery/Camera Roll)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideoUri(
                uri = uri,
                type = pendingAnalysisType,
                speed = pendingAnalysisSpeed,
                runInBackground = pendingRunInBackground,
                enableSkeleton = pendingEnableSkeleton
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val state by viewModel.uiState.collectAsState()

    // Dialog to configure and upload video (Shooting vs Tactical Match + Speed Selector)
    if (showVideoSourceDialog) {
        VideoUploadDialog(
            initialType = state.activeAnalysisType,
            initialSpeed = state.activeAnalysisSpeed,
            initialEnableSkeleton = state.showSkeleton,
            savedVideosCount = state.savedVideos.size,
            onDismiss = { showVideoSourceDialog = false },
            onOpenSavedVideos = { viewModel.setSavedVideosDialogVisible(true) },
            onPickFromGallery = { type, speed, inBackground, enableSkeleton ->
                pendingAnalysisType = type
                pendingAnalysisSpeed = speed
                pendingRunInBackground = inBackground
                pendingEnableSkeleton = enableSkeleton
                if (inBackground) {
                    requestNotificationPermissionIfNeeded()
                }
                galleryPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onPickFromFilePicker = { type, speed, inBackground, enableSkeleton ->
                pendingAnalysisType = type
                pendingAnalysisSpeed = speed
                pendingRunInBackground = inBackground
                pendingEnableSkeleton = enableSkeleton
                if (inBackground) {
                    requestNotificationPermissionIfNeeded()
                }
                filePickerLauncher.launch(arrayOf("video/*", "video/mp4", "application/octet-stream", "*/*"))
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Loading & Pre-Analysis overlay when analyzing video in foreground
        if ((state.isVideoLoading || state.isVideoAnalyzing) && !state.isVideoAnalysisInBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF50B0B0E))
                    .safeDrawingPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF13131A))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { state.videoAnalysisProgress },
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 5.dp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ANALIZANDO VÍDEO CON IA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${(state.videoAnalysisProgress * 100).toInt()}% COMPLETADO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { state.videoAnalysisProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0x22FFFFFF)
                    )

                    Text(
                        text = state.videoAnalysisStatus.ifEmpty { "Extrayendo fotogramas y calculando trayectoria..." },
                        fontSize = 12.sp,
                        color = Color(0xCCFFFFFF),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Precalculando esqueleto MediaPipe y detección de balón para garantizar reproducción a 60 FPS fluida.",
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option to send analysis to background
                    Box(
                        modifier = Modifier
                            .testTag("run_analysis_in_background_button")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00E5FF))
                            .clickable {
                                requestNotificationPermissionIfNeeded()
                                viewModel.setVideoAnalysisInBackground(true)
                            }
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SEGUIR EN SEGUNDO PLANO",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }

                    Text(
                        text = "Te avisaremos con una notificación cuando el análisis termine mientras sigues usando la app.",
                        fontSize = 11.sp,
                        color = Color(0xAAFFFFFF),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )

                    Box(
                        modifier = Modifier
                            .testTag("cancel_video_analysis_button")
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { viewModel.cancelVideoAnalysis() }
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancelar análisis",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                }
            }
        }
        // 1. VIDEO FILE MODE: Directly play and analyze loaded video
        else if (state.inputMode == InputMode.VIDEO_FILE && state.selectedVideoUri != null) {
            VideoPlayerAnalyzer(
                videoUri = state.selectedVideoUri!!,
                state = state,
                onFrameAnalyzed = { bitmap -> viewModel.onFrameAnalyzed(bitmap) },
                onHoopSelected = { nx, ny -> viewModel.onManualHoopSelected(nx, ny) },
                onConfirmLockHoop = { viewModel.confirmLockHoop() },
                onBackToLiveCamera = { viewModel.switchToLiveCamera() },
                onPickAnotherVideo = { showVideoSourceDialog = true },
                onOpenSavedVideos = { viewModel.setSavedVideosDialogVisible(true) },
                onResetStats = { viewModel.reset() },
                debug = debugMode,
                onToggleDebug = { debugMode = !debugMode },
                onToggleHoopVisual = { viewModel.toggleHoopVisual() },
                onShowTacticalReport = { viewModel.showTacticalReport() },
                onToggleShowSkeleton = { viewModel.toggleShowSkeleton() },
                onToggleShowHoop = { viewModel.toggleShowHoop() },
                onToggleShowBall = { viewModel.toggleShowBall() },
                onCycleHoopPerspective = { viewModel.cycleHoopPerspective() },
                onSetHoopPerspective = { persp -> viewModel.setHoopPerspective(persp) },
                modifier = Modifier.fillMaxSize()
            )
        }
        // 2. LIVE CAMERA MODE: Requires camera permission
        else if (!hasCameraPermission) {
            PermissionGate(
                denied = permissionDenied,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onPickVideo = { showVideoSourceDialog = true }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vision_test_screen")
            ) {
                // Camera Stream with concurrent video recording support
                CameraPreview(
                    onFrameAnalyzed = { bitmap ->
                        viewModel.onFrameAnalyzed(bitmap)
                    },
                    onCameraStateChanged = { active ->
                        viewModel.setCameraActive(active)
                    },
                    useFrontCamera = state.useFrontCamera,
                    isRecording = state.isRecordingLive,
                    onVideoRecorded = { uri ->
                        viewModel.onVideoRecorded(uri)
                    }
                )

                if (state.isKidsMiniBasketMode) {
                    KidsMiniBasketHUD(
                        state = state,
                        frozenBitmap = viewModel.kidsFrozenBitmap,
                        onAdvanceToPlacePhone = { viewModel.advanceToPlacePhoneStep() },
                        onTakePhoto = { viewModel.takeKidsCalibrationPhoto() },
                        onBackToScanBall = { viewModel.returnToScanBallStep() },
                        onBackToPlacePhone = { viewModel.returnToPlacePhoneStep() },
                        onUpdateHoopPosition = { x, y -> viewModel.updateKidsHoopPosition(x, y) },
                        onUpdateHoopRadius = { r -> viewModel.updateKidsHoopRadius(r) },
                        onConfirmHoopAndStart = { viewModel.confirmKidsCalibrationAndStart() },
                        onRestartSession = { viewModel.startKidsMiniBasketSessionNow() },
                        onRecalibrate = { viewModel.recalibrateKidsMiniBasket() },
                        onManualScoreBasket = { viewModel.manualScoreKidsBasket() },
                        onExitToMain = onReopenOnboarding,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.calibrationStep != CalibrationStep.COMPLETED) {
                    CalibrationWizard(
                        currentStep = state.calibrationStep,
                        suggestedHoop = state.lockedHoop,
                        ballDetected = state.ballCalibrationProgress > 0.15f || (state.ball?.conf ?: 0f) > 0.35f,
                        ballProgress = state.ballCalibrationProgress,
                        isDribbleMode = state.isDribbleMode || state.isReactionPointsMode || state.isDefendZoneMode,
                        skeletonDetected = state.skeleton?.landmarks?.isNotEmpty() == true,
                        skeletonProgress = state.skeletonCalibrationProgress,
                        skeletonFeedback = state.skeletonCalibrationFeedback,
                        ballFeedback = state.ballCalibrationFeedback,
                        courtCalibration = state.courtCalibration,
                        onHoopSelected = { nx, ny -> viewModel.onManualHoopSelected(nx, ny) },
                        onLockHoopConfirmed = { viewModel.confirmLockHoop() },
                        onPerspectiveChanged = { persp -> viewModel.setHoopPerspective(persp) },
                        onCourtPointMoved = { id, nx, ny -> viewModel.setCourtPoint(id, nx, ny) },
                        onCourtPresetApplied = { preset -> viewModel.applyCourtPreset(preset) },
                        onCourtReset = { viewModel.resetCourtPoints() },
                        onFastConfirmBall = { viewModel.forceCompleteBallCalibration() },
                        onNextStep = {
                            when (state.calibrationStep) {
                                CalibrationStep.POSITION_PHONE -> viewModel.setCalibrationStep(CalibrationStep.LOCK_HOOP)
                                CalibrationStep.LOCK_HOOP -> viewModel.confirmLockHoop()
                                CalibrationStep.CALIBRATE_SKELETON -> viewModel.setCalibrationStep(CalibrationStep.CHECK_BALL)
                                CalibrationStep.CHECK_BALL -> {
                                    if (state.isDribbleMode || state.isReactionPointsMode || state.isDefendZoneMode) {
                                        viewModel.setCalibrationStep(CalibrationStep.COMPLETED)
                                    } else {
                                        viewModel.setCalibrationStep(CalibrationStep.COURT_ALIGNMENT)
                                    }
                                }
                                CalibrationStep.COURT_ALIGNMENT -> viewModel.setCalibrationStep(CalibrationStep.COMPLETED)
                                CalibrationStep.COMPLETED -> Unit
                            }
                        },
                        onSkipCalibration = { viewModel.setCalibrationStep(CalibrationStep.COMPLETED) },
                        onExitToMain = onReopenOnboarding,
                        onPickVideo = { showVideoSourceDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isDefendZoneMode) {
                    DefendZoneHUD(
                        state = state,
                        onSelectThreatType = { type -> viewModel.setDefendThreatType(type) },
                        onDismissPopup = { id -> viewModel.dismissDefendPopup(id) },
                        onRestartDrill = { viewModel.restartDefendZoneMode() },
                        onExitToMain = onReopenOnboarding,
                        onToggleShowSkeleton = { viewModel.toggleShowSkeleton() },
                        onToggleShowHoop = { viewModel.toggleShowHoop() },
                        onToggleShowBall = { viewModel.toggleShowBall() },
                        onToggleCamera = { viewModel.toggleCameraLens() },
                        onToggleRecording = { viewModel.toggleLiveRecording() },
                        onRecalibrate = { viewModel.recalibrateDefendZone() },
                        onSelectDribbleCombo = { viewModel.startDribbleMode() },
                        onSelectReactionPoints = { viewModel.startReactionPointsMode() },
                        onSelectDefendZone = { viewModel.restartDefendZoneMode() },
                        onSelectShooting = { viewModel.startShootingModeWithTutorial() },
                        onSelectUploadVideo = { showVideoSourceDialog = true },
                        onManualEvade = { id -> viewModel.manualEvadeThreat(id) },
                        onManualSteal = { id -> viewModel.manualStealThreat(id) },
                        onManualShield = { id -> viewModel.manualBodyShieldThreat(id) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isReactionPointsMode) {
                    ReactionPointsHUD(
                        state = state,
                        onHitPoint = { id -> viewModel.hitReactionPoint(id) },
                        onDismissPopup = { id -> viewModel.dismissReactionPopup(id) },
                        onRestartDrill = { viewModel.restartReactionPointsMode() },
                        onExitToMain = onReopenOnboarding,
                        onToggleShowSkeleton = { viewModel.toggleShowSkeleton() },
                        onToggleShowHoop = { viewModel.toggleShowHoop() },
                        onToggleShowBall = { viewModel.toggleShowBall() },
                        onToggleCamera = { viewModel.toggleCameraLens() },
                        onToggleRecording = { viewModel.toggleLiveRecording() },
                        onRecalibrate = { viewModel.recalibrateFrontDrills() },
                        onSelectDribbleCombo = { viewModel.startDribbleMode() },
                        onSelectReactionPoints = { viewModel.restartReactionPointsMode() },
                        onSelectDefendZone = { viewModel.startDefendZoneMode() },
                        onSelectShooting = { viewModel.startShootingModeWithTutorial() },
                        onSelectUploadVideo = { showVideoSourceDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isDribbleMode) {
                    DribbleComboHUD(
                        state = state,
                        onTriggerCrossover = { viewModel.triggerDribbleCrossover() },
                        onDismissPopup = { id -> viewModel.dismissDribblePopup(id) },
                        onDismissSmokePuff = { id -> viewModel.dismissDribbleSmokePuff(id) },
                        onRestartDrill = { viewModel.restartDribbleMode() },
                        onExitToMain = onReopenOnboarding,
                        onToggleShowSkeleton = { viewModel.toggleShowSkeleton() },
                        onToggleShowHoop = { viewModel.toggleShowHoop() },
                        onToggleShowBall = { viewModel.toggleShowBall() },
                        onToggleCamera = { viewModel.toggleCameraLens() },
                        onToggleRecording = { viewModel.toggleLiveRecording() },
                        onRecalibrate = { viewModel.recalibrateFrontDrills() },
                        onToggleShowFps = { viewModel.toggleShowFps() },
                        onSelectDribbleCombo = { viewModel.restartDribbleMode() },
                        onSelectReactionPoints = { viewModel.startReactionPointsMode() },
                        onSelectDefendZone = { viewModel.startDefendZoneMode() },
                        onSelectShooting = { viewModel.startShootingModeWithTutorial() },
                        onSelectUploadVideo = { showVideoSourceDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VisionHUD(
                        state = state,
                        cameraReady = state.cameraActive,
                        debug = debugMode,
                        onToggleDebug = { debugMode = !debugMode },
                        onReset = {
                            if (state.isDribbleMode) viewModel.resetDribbleScore() else viewModel.reset()
                        },
                        onRecalibrateHoop = { viewModel.recalibrate() },
                        onSimulateShot = { make -> viewModel.simulateShot(make = make) },
                        onToggleRecording = { viewModel.toggleLiveRecording() },
                        onPickVideo = { showVideoSourceDialog = true },
                        onAnalyzeRecordedVideo = { viewModel.analyzeRecordedVideoNow() },
                        onDismissVideoBanner = { viewModel.dismissRecordedVideoBanner() },
                        onToggleHoopVisual = { viewModel.toggleHoopVisual() },
                        onToggleTacticalMode = { viewModel.toggleTacticalMode() },
                        onShowTacticalReport = { viewModel.showTacticalReport() },
                        onSimulateTactical = { viewModel.simulateTacticalPlays() },
                        onOpenSavedVideos = { viewModel.setSavedVideosDialogVisible(true) },
                        onReopenOnboarding = onReopenOnboarding,
                        onTriggerDribbleCrossover = { viewModel.triggerDribbleCrossover() },
                        onDismissDribblePopup = { id -> viewModel.dismissDribblePopup(id) },
                        onToggleDribbleMode = { viewModel.toggleDribbleMode() },
                        onToggleShowSkeleton = { viewModel.toggleShowSkeleton() },
                        onToggleShowHoop = { viewModel.toggleShowHoop() },
                        onToggleShowBall = { viewModel.toggleShowBall() },
                        onCycleHoopPerspective = { viewModel.cycleHoopPerspective() },
                        onSetHoopPerspective = { persp -> viewModel.setHoopPerspective(persp) },
                        onToggleShowFps = { viewModel.toggleShowFps() },
                        onToggleCamera = { viewModel.toggleCameraLens() },
                        onOpenCourtAlignment = { viewModel.setShowCourtPointSelector(true) },
                        onCourtPointMoved = { id, nx, ny -> viewModel.setCourtPoint(id, nx, ny) },
                        onCourtPresetApplied = { preset -> viewModel.applyCourtPreset(preset) },
                        onCourtReset = { viewModel.resetCourtPoints() },
                        onCloseCourtAlignment = { viewModel.setShowCourtPointSelector(false) },
                        onSelectDribbleCombo = { viewModel.startDribbleMode() },
                        onSelectReactionPoints = { viewModel.startReactionPointsMode() },
                        onSelectDefendZone = { viewModel.startDefendZoneMode() },
                        onSelectKidsMiniBasket = { viewModel.startKidsMiniBasketMode() },
                        onSelectShooting = { viewModel.startShootingModeWithTutorial() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Overlay de "¡JUGAR AHORA!" y cuenta regresiva de 3 segundos para todos los modos de juego
            if ((state.calibrationStep == CalibrationStep.COMPLETED || (state.isKidsMiniBasketMode && state.kidsCalibrationStep == KidsCalibrationStep.COMPLETED)) && (state.isAwaitingPlayStart || state.playStartCountdownSec != null)) {
                PlayNowCountdownOverlay(
                    isAwaitingPlayStart = state.isAwaitingPlayStart,
                    countdownSec = state.playStartCountdownSec,
                    isGameMode = state.isDribbleMode || state.isReactionPointsMode || state.isDefendZoneMode || state.isKidsMiniBasketMode,
                    onPlayNow = { viewModel.startPlayCountdown() },
                    onExitToMain = {
                        viewModel.cancelPlayCountdown()
                        onReopenOnboarding()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(45f)
                )
            }
        }

        // Floating In-App Notifications and Background Progress Banner (Top Layer)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(50f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isVideoAnalyzing && state.isVideoAnalysisInBackground) {
                BackgroundAnalysisBanner(
                    progress = state.videoAnalysisProgress,
                    analyzedFrames = state.videoAnalyzedFrames,
                    totalFrames = state.videoTotalFrames,
                    onOpen = { viewModel.setVideoAnalysisInBackground(false) },
                    onCancel = { viewModel.cancelVideoAnalysis() }
                )
            }

            if (state.videoAnalysisCompletedNotice != null) {
                VideoAnalysisCompletedNoticeCard(
                    notice = state.videoAnalysisCompletedNotice!!,
                    onOpenVideo = { viewModel.openCompletedVideoFromNotice() },
                    onDismiss = { viewModel.dismissVideoAnalysisCompletedNotice() }
                )
            }
        }
    }

    // TACTICAL MATCH REPORT DIALOG (Informe táctico completo al terminar análisis o a demanda)
    if (state.showTacticalReportDialog && state.tacticalReport != null) {
        TacticalReportDialog(
            report = state.tacticalReport!!,
            onDismiss = { viewModel.dismissTacticalReportDialog() }
        )
    }

    // MIS VÍDEOS DIALOG (Biblioteca de análisis guardados)
    if (state.showSavedVideosDialog) {
        SavedVideosDialog(
            savedVideos = state.savedVideos,
            onDismiss = { viewModel.setSavedVideosDialogVisible(false) },
            onOpenVideo = { saved -> viewModel.openSavedVideo(saved) },
            onDeleteVideo = { id -> viewModel.deleteSavedVideo(id) },
            onUploadNewVideo = { showVideoSourceDialog = true }
        )
    }
}

@Composable
fun VideoSourceDialog(
    onDismiss: () -> Unit,
    onPickFromFiles: (runInBackground: Boolean) -> Unit,
    onPickFromGallery: (runInBackground: Boolean) -> Unit
) {
    var runInBackground by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SELECCIONAR VÍDEO (.mp4)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Sin límite de tamaño (14 MB, 50 MB, etc.)",
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Elige de dónde quieres cargar tu vídeo de tiros:",
                    fontSize = 13.sp,
                    color = Color(0xCCFFFFFF)
                )

                // Option 1: Files / Downloads / WhatsApp / Drive (Guaranteed to find 14MB mp4)
                Box(
                    modifier = Modifier
                        .testTag("pick_from_files_option")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x3300E5FF))
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                        .clickable { onPickFromFiles(runInBackground) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Descargas / Explorador de Archivos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Recomendado si descargaste el vídeo, te lo pasaron por WhatsApp/Drive o está en la carpeta Descargas.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xDDFFFFFF)
                            )
                        }
                    }
                }

                // Option 2: Gallery / Camera Roll
                Box(
                    modifier = Modifier
                        .testTag("pick_from_gallery_option")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onPickFromGallery(runInBackground) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Galería / Fotos del Móvil",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Para vídeos grabados directamente con la cámara del dispositivo.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }
                    }
                }

                // Background Mode Option Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (runInBackground) Color(0x2A00E5FF) else Color(0x14FFFFFF))
                        .border(
                            1.dp,
                            if (runInBackground) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { runInBackground = !runInBackground }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = runInBackground,
                            onCheckedChange = { runInBackground = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00E5FF)
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Analizar en segundo plano",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "No bloqueará la pantalla y te avisará con notificación y aviso en la app al completarse.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundAnalysisBanner(
    progress: Float,
    analyzedFrames: Int,
    totalFrames: Int,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF0121420))
            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0x33FFFFFF),
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 3.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Analizando vídeo en 2º plano",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF)
                    )
                }
                Text(
                    text = if (totalFrames > 0) "Fotograma $analyzedFrames de $totalFrames" else "Extrayendo fotogramas...",
                    fontSize = 11.sp,
                    color = Color(0xAAFFFFFF)
                )
            }

            // Expand button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3300E5FF))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .clickable { onOpen() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "VER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
            }

            // Cancel button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar análisis",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VideoAnalysisCompletedNoticeCard(
    notice: VideoAnalysisCompletedNotice,
    onOpenVideo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF8151726))
            .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏀",
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "¡ANÁLISIS COMPLETADO!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = notice.summaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xDDFFFFFF)
            )

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${notice.totalFrames} frames",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (notice.attempts > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x334CAF50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${notice.makes}/${notice.attempts} tiros",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }

                if (notice.tacticalPlayCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3300E5FF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${notice.tacticalPlayCount} tácticas",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E5FF))
                        .clickable { onOpenVideo() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VER VÍDEO Y REPORTE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22FFFFFF))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MÁS TARDE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionGate(
    denied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickVideo: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SportCanvas)
            .safeDrawingPadding()
            .testTag("permission_gate")
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = SportOrange,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.permission_body),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = SportOnSurfaceSecondary,
                textAlign = TextAlign.Center
            )

            if (denied) {
                Text(
                    text = stringResource(R.string.permission_denied),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SportWarning,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .testTag("open_settings_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SportOrange)
                        .clickable { onOpenSettings() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_open_settings),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = SportOnBrand
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag("retry_permission_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SportBorder, RoundedCornerShape(14.dp))
                        .clickable { onRequestPermission() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_try_again),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = SportOnSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .testTag("enable_camera_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SportOrange)
                        .clickable { onRequestPermission() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_enable_camera),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = SportOnBrand
                    )
                }
            }

            // Always allow testing with a pre-recorded video
            Box(
                modifier = Modifier
                    .testTag("gate_pick_video_button")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                    .clickable { onPickVideo() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "PROBAR CON VÍDEO GRABADO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}
