package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CompassCalibration
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.MeasurementRepository
import com.example.ui.AppScreen
import com.example.ui.MeasureViewModel
import com.example.ui.caliper.ScreenCaliperScreen
import com.example.ui.camera.LiveCameraCaptureScreen
import com.example.ui.clinometer.ClinometerRangefinderScreen
import com.example.ui.history.MeasurementHistoryScreen
import com.example.ui.studio.PhotoMeasureStudioScreen
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(rtl = true) {
                OpticalMeasureApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalMeasureApp() {
    val context = LocalContext.current.applicationContext
    val database = AppDatabase.getInstance(context)
    val repository = MeasurementRepository(database.measurementDao())
    val viewModel: MeasureViewModel = viewModel(
        factory = MeasureViewModel.Factory(repository, context)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val liveTilt by viewModel.tiltTelemetry.collectAsStateWithLifecycle()
    val savedRecords by viewModel.savedMeasurements.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianBg,
        topBar = {
            if (uiState.currentScreen != AppScreen.CAMERA) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateSurface,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Architecture,
                                contentDescription = null,
                                tint = LaserCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "مقياس الكاميرا الذكي",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "معايرة بصرية حقيقية • حساسات الميل والعدسة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        // Live Spirit Level indicator pill in TopBar
                        Surface(
                            color = SlateSurfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (liveTilt.isSteadyAndAligned) LevelEmerald else PrecisionAmber
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "ميزان الاستواء",
                                    tint = if (liveTilt.isSteadyAndAligned) LevelEmerald else PrecisionAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f°", liveTilt.flatTiltDeg)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(AppScreen.STUDIO, "بالصورة", Icons.Filled.Architecture to Icons.Outlined.Architecture),
                    Triple(AppScreen.CAMERA, "الكاميرا", Icons.Filled.CameraAlt to Icons.Outlined.CameraAlt),
                    Triple(AppScreen.CLINOMETER, "المسافات", Icons.Filled.CompassCalibration to Icons.Outlined.CompassCalibration),
                    Triple(AppScreen.CALIPER, "المسطرة", Icons.Filled.Straighten to Icons.Outlined.Straighten),
                    Triple(AppScreen.HISTORY, "السجل", Icons.Filled.History to Icons.Outlined.History)
                )

                navItems.forEach { (screen, label, icons) ->
                    val selected = uiState.currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = {
                            Icon(
                                imageVector = if (selected) icons.first else icons.second,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBg,
                            selectedTextColor = LaserCyan,
                            indicatorColor = LaserCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.STUDIO -> PhotoMeasureStudioScreen(
                    uiState = uiState,
                    liveTilt = liveTilt,
                    onSelectUnit = viewModel::selectUnit,
                    onSetCalibrationMode = viewModel::setCalibrationMode,
                    onSelectPreset = viewModel::selectReferencePreset,
                    onUpdateCustomRefCm = viewModel::updateCustomReferenceCm,
                    onUpdateDistanceCm = viewModel::updateObjectDistanceCm,
                    onTogglePerspective = viewModel::togglePerspectiveCorrection,
                    onUpdateRefPoints = viewModel::updateReferencePoints,
                    onUpdateLinePoints = viewModel::updateMeasurementLinePoints,
                    onSelectLine = viewModel::selectMeasurementLine,
                    onAddLine = viewModel::addMeasurementLine,
                    onRemoveLine = viewModel::removeMeasurementLine,
                    onLoadScene = viewModel::loadWorkbenchScene,
                    onOpenLiveCamera = { viewModel.navigateTo(AppScreen.CAMERA) },
                    onPhotoPicked = viewModel::onPhotoCapturedOrPicked,
                    onNavigateToClinometer = { viewModel.navigateTo(AppScreen.CLINOMETER) },
                    onSaveMeasurement = viewModel::saveCurrentStudioMeasurement,
                    onDismissBanner = viewModel::clearBanner
                )

                AppScreen.CAMERA -> LiveCameraCaptureScreen(
                    optics = uiState.cameraOptics,
                    tiltTelemetry = liveTilt,
                    onPhotoCaptured = viewModel::onPhotoCapturedOrPicked,
                    onUseWorkbenchScene = viewModel::loadWorkbenchScene,
                    onBack = { viewModel.navigateTo(AppScreen.STUDIO) }
                )

                AppScreen.CLINOMETER -> ClinometerRangefinderScreen(
                    uiState = uiState,
                    liveTilt = liveTilt,
                    onUpdateCameraHeight = viewModel::updateCameraHeightCm,
                    onUpdateBaseAngle = viewModel::lockOrUpdateBaseAngle,
                    onUpdateTopAngle = viewModel::lockOrUpdateTopAngle,
                    onApplyDistanceToStudio = viewModel::applyDistanceFromClinometerToStudio,
                    onSaveClinometerMeasurement = viewModel::saveClinometerMeasurement,
                    onBack = { viewModel.navigateTo(AppScreen.STUDIO) }
                )

                AppScreen.CALIPER -> ScreenCaliperScreen(
                    uiState = uiState,
                    liveTilt = liveTilt,
                    onUpdateCaliperMm = viewModel::updateCaliperLengthMm,
                    onUpdateDpiMultiplier = viewModel::updateDpiMultiplier,
                    onBack = { viewModel.navigateTo(AppScreen.STUDIO) }
                )

                AppScreen.HISTORY -> MeasurementHistoryScreen(
                    records = savedRecords,
                    selectedUnit = uiState.selectedUnit,
                    onDeleteRecord = viewModel::deleteRecord,
                    onBack = { viewModel.navigateTo(AppScreen.STUDIO) }
                )
            }
        }
    }
}
