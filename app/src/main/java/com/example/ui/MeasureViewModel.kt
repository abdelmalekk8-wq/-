package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MeasurementRecord
import com.example.data.MeasurementRepository
import com.example.domain.CalibrationMode
import com.example.domain.MeasureUnit
import com.example.domain.MeasurementLine
import com.example.domain.NormalizedPoint
import com.example.domain.PhotogrammetryEngine
import com.example.domain.ReferenceObjectPreset
import com.example.domain.WorkbenchScene
import com.example.sensors.CameraOpticalSpecs
import com.example.sensors.DeviceOpticsAndSensors
import com.example.sensors.DeviceTiltTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    STUDIO,      // قياس أي شيء من صورته (معايرة بمرجع أو ببصريات الكاميرا)
    CAMERA,      // الكاميرا الحية لالتقاط صورة مستوية مع ميزان ليزري
    CLINOMETER,  // قياس المسافات والارتفاعات الكبيرة بتثليث الزوايا
    CALIPER,     // مسطرة الشاشة الحقيقية 1:1 وميزان الاستواء
    HISTORY      // سجل القياسات المحفوظة
}

data class StudioUiState(
    val currentScreen: AppScreen = AppScreen.STUDIO,
    val selectedUnit: MeasureUnit = MeasureUnit.CM,
    val capturedPhotoUri: Uri? = null,
    val activeWorkbenchScene: WorkbenchScene = WorkbenchScene.DESK_SETUP,
    val calibrationMode: CalibrationMode = CalibrationMode.REFERENCE_OBJECT,
    val selectedPreset: ReferenceObjectPreset = ReferenceObjectPreset.BANK_CARD_WIDTH,
    val customReferenceCm: Double = 10.0,
    val referenceStart: NormalizedPoint = WorkbenchScene.DESK_SETUP.defaultRefStart,
    val referenceEnd: NormalizedPoint = WorkbenchScene.DESK_SETUP.defaultRefEnd,
    val measurementLines: List<MeasurementLine> = WorkbenchScene.DESK_SETUP.defaultMeasurements,
    val selectedLineId: Int = 1,
    val objectDistanceCm: Double = 55.0,
    val capturedTiltDeg: Float = 1.2f,
    val applyPerspectiveCorrection: Boolean = true,
    val cameraOptics: CameraOpticalSpecs = CameraOpticalSpecs(),
    // Clinometer Rangefinder state
    val cameraHeightCm: Double = 150.0,
    val lockedBaseDepressionDeg: Double = 32.0,
    val lockedTopElevationDeg: Double = 8.5,
    val isBaseAngleLocked: Boolean = true,
    val isTopAngleLocked: Boolean = true,
    // Screen 1:1 Caliper state
    val caliperLengthMm: Float = 45.0f,
    val dpiMultiplier: Float = 1.0f,
    val statusBannerMessage: String? = null
) {
    val effectiveReferenceCm: Double
        get() = if (selectedPreset == ReferenceObjectPreset.CUSTOM) {
            customReferenceCm.coerceAtLeast(0.1)
        } else {
            selectedPreset.realLengthCm
        }

    fun computeLineCm(line: MeasurementLine, imageAspectRatio: Float = 0.75f): Double {
        return PhotogrammetryEngine.calculateRealLengthCm(
            targetLine = line,
            calibrationMode = calibrationMode,
            referenceStart = referenceStart,
            referenceEnd = referenceEnd,
            referenceRealCm = effectiveReferenceCm,
            objectDistanceCm = objectDistanceCm,
            focalLengthMm = cameraOptics.focalLengthMm,
            sensorWidthMm = cameraOptics.sensorWidthMm,
            tiltAngleDeg = capturedTiltDeg,
            applyPerspectiveCorrection = applyPerspectiveCorrection,
            imageAspectRatio = imageAspectRatio
        )
    }

    val confidencePercent: Int
        get() {
            val refNormSpan = MeasurementLine(
                id = 0,
                label = "Ref",
                start = referenceStart,
                end = referenceEnd
            ).normalizedDistance(0.75f)
            return PhotogrammetryEngine.estimateConfidencePercent(
                calibrationMode = calibrationMode,
                referenceNormalizedSpan = refNormSpan,
                tiltAngleDeg = capturedTiltDeg
            )
        }
}

class MeasureViewModel(
    private val repository: MeasurementRepository,
    appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudioUiState(
            cameraOptics = DeviceOpticsAndSensors.readBackCameraOptics(appContext)
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val tiltTelemetry: StateFlow<DeviceTiltTelemetry> =
        DeviceOpticsAndSensors.observeDeviceTilt(appContext)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DeviceTiltTelemetry()
            )

    val savedMeasurements: StateFlow<List<MeasurementRecord>> =
        repository.allMeasurements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedInitialExamplesIfEmpty()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, statusBannerMessage = null) }
    }

    fun selectUnit(unit: MeasureUnit) {
        _uiState.update { it.copy(selectedUnit = unit) }
    }

    fun setCalibrationMode(mode: CalibrationMode) {
        _uiState.update { it.copy(calibrationMode = mode) }
    }

    fun selectReferencePreset(preset: ReferenceObjectPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                calibrationMode = CalibrationMode.REFERENCE_OBJECT
            )
        }
    }

    fun updateCustomReferenceCm(cm: Double) {
        _uiState.update {
            it.copy(
                customReferenceCm = cm.coerceIn(0.1, 5000.0),
                selectedPreset = ReferenceObjectPreset.CUSTOM
            )
        }
    }

    fun updateObjectDistanceCm(distanceCm: Double) {
        _uiState.update {
            it.copy(objectDistanceCm = distanceCm.coerceIn(5.0, 2000.0))
        }
    }

    fun togglePerspectiveCorrection(enabled: Boolean) {
        _uiState.update { it.copy(applyPerspectiveCorrection = enabled) }
    }

    fun updateReferencePoints(start: NormalizedPoint, end: NormalizedPoint) {
        _uiState.update {
            it.copy(
                referenceStart = NormalizedPoint(
                    start.x.coerceIn(0.03f, 0.97f),
                    start.y.coerceIn(0.03f, 0.97f)
                ),
                referenceEnd = NormalizedPoint(
                    end.x.coerceIn(0.03f, 0.97f),
                    end.y.coerceIn(0.03f, 0.97f)
                )
            )
        }
    }

    fun updateMeasurementLinePoints(lineId: Int, start: NormalizedPoint, end: NormalizedPoint) {
        _uiState.update { state ->
            val updated = state.measurementLines.map { line ->
                if (line.id == lineId) {
                    line.copy(
                        start = NormalizedPoint(
                            start.x.coerceIn(0.03f, 0.97f),
                            start.y.coerceIn(0.03f, 0.97f)
                        ),
                        end = NormalizedPoint(
                            end.x.coerceIn(0.03f, 0.97f),
                            end.y.coerceIn(0.03f, 0.97f)
                        )
                    )
                } else {
                    line
                }
            }
            state.copy(measurementLines = updated, selectedLineId = lineId)
        }
    }

    fun selectMeasurementLine(lineId: Int) {
        _uiState.update { it.copy(selectedLineId = lineId) }
    }

    fun addMeasurementLine() {
        _uiState.update { state ->
            val nextId = (state.measurementLines.maxOfOrNull { it.id } ?: 0) + 1
            val palette = listOf(0xFFFFB703L, 0xFFA855F7L, 0xFF10B981L, 0xFFF43F5EL)
            val color = palette[(nextId - 1) % palette.size]
            val offset = ((nextId % 4) * 0.06f)
            val newLine = MeasurementLine(
                id = nextId,
                label = "القياس #$nextId",
                start = NormalizedPoint((0.25f + offset).coerceAtMost(0.85f), (0.42f + offset).coerceAtMost(0.85f)),
                end = NormalizedPoint((0.72f + offset).coerceAtMost(0.92f), (0.42f + offset).coerceAtMost(0.85f)),
                colorHex = color
            )
            state.copy(
                measurementLines = state.measurementLines + newLine,
                selectedLineId = nextId
            )
        }
    }

    fun removeMeasurementLine(lineId: Int) {
        _uiState.update { state ->
            if (state.measurementLines.size <= 1) return@update state
            val remaining = state.measurementLines.filterNot { it.id == lineId }
            state.copy(
                measurementLines = remaining,
                selectedLineId = remaining.first().id
            )
        }
    }

    fun loadWorkbenchScene(scene: WorkbenchScene) {
        _uiState.update {
            it.copy(
                capturedPhotoUri = null,
                activeWorkbenchScene = scene,
                selectedPreset = scene.defaultPreset,
                objectDistanceCm = scene.defaultDistanceCm,
                referenceStart = scene.defaultRefStart,
                referenceEnd = scene.defaultRefEnd,
                measurementLines = scene.defaultMeasurements,
                selectedLineId = scene.defaultMeasurements.firstOrNull()?.id ?: 1,
                capturedTiltDeg = 1.1f,
                currentScreen = AppScreen.STUDIO,
                statusBannerMessage = "تم تحميل مشهد: ${scene.arabicName}"
            )
        }
    }

    fun onPhotoCapturedOrPicked(uri: Uri, capturedTiltDeg: Float) {
        _uiState.update {
            it.copy(
                capturedPhotoUri = uri,
                capturedTiltDeg = capturedTiltDeg,
                currentScreen = AppScreen.STUDIO,
                statusBannerMessage = "تم استيراد الصورة وتسجيل زاوية الميل (${String.format(java.util.Locale.US, "%.1f°", capturedTiltDeg)})"
            )
        }
    }

    fun applyDistanceFromClinometerToStudio(distanceCm: Double) {
        _uiState.update {
            it.copy(
                objectDistanceCm = distanceCm.coerceIn(10.0, 2000.0),
                calibrationMode = CalibrationMode.CAMERA_OPTICS_AND_DISTANCE,
                currentScreen = AppScreen.STUDIO,
                statusBannerMessage = "تم ضبط مسافة الهدف على ${String.format(java.util.Locale.US, "%.1f", distanceCm)} سم في استوديو القياس"
            )
        }
    }

    // Clinometer controls
    fun updateCameraHeightCm(heightCm: Double) {
        _uiState.update { it.copy(cameraHeightCm = heightCm.coerceIn(30.0, 250.0)) }
    }

    fun lockOrUpdateBaseAngle(angleDeg: Double) {
        _uiState.update {
            it.copy(
                lockedBaseDepressionDeg = angleDeg.coerceIn(4.0, 86.0),
                isBaseAngleLocked = true
            )
        }
    }

    fun lockOrUpdateTopAngle(angleDeg: Double) {
        _uiState.update {
            it.copy(
                lockedTopElevationDeg = angleDeg.coerceIn(-80.0, 85.0),
                isTopAngleLocked = true
            )
        }
    }

    // Screen 1:1 Caliper controls
    fun updateCaliperLengthMm(lengthMm: Float) {
        _uiState.update { it.copy(caliperLengthMm = lengthMm.coerceIn(2f, 160f)) }
    }

    fun updateDpiMultiplier(multiplier: Float) {
        _uiState.update { it.copy(dpiMultiplier = multiplier.coerceIn(0.8f, 1.2f)) }
    }

    fun saveCurrentStudioMeasurement(customTitle: String) {
        val state = _uiState.value
        val primaryLine = state.measurementLines.find { it.id == state.selectedLineId }
            ?: state.measurementLines.firstOrNull()
            ?: return
        val primaryCm = state.computeLineCm(primaryLine)
        val secondaryLine = state.measurementLines.firstOrNull { it.id != primaryLine.id }
        val secondaryCm = secondaryLine?.let { state.computeLineCm(it) }

        val title = customTitle.trim().ifEmpty { primaryLine.label }
        val refSummary = if (state.calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
            "${state.selectedPreset.arabicTitle} (${String.format(java.util.Locale.US, "%.2f", state.effectiveReferenceCm)} سم) • ميل ${String.format(java.util.Locale.US, "%.1f°", state.capturedTiltDeg)}"
        } else {
            "عدسة ${String.format(java.util.Locale.US, "%.2f", state.cameraOptics.focalLengthMm)} ملم • مسافة ${String.format(java.util.Locale.US, "%.0f", state.objectDistanceCm)} سم"
        }

        viewModelScope.launch {
            repository.insert(
                MeasurementRecord(
                    title = title,
                    primaryLengthCm = primaryCm,
                    secondaryLengthCm = secondaryCm,
                    calibrationMethod = state.calibrationMode.arabicTitle,
                    referenceDetails = refSummary,
                    confidencePercent = state.confidencePercent,
                    notes = if (state.capturedPhotoUri != null) "قياس من صورة ملتقطة" else "قياس من ${state.activeWorkbenchScene.arabicName}"
                )
            )
            _uiState.update {
                it.copy(statusBannerMessage = "تم حفظ قياس \"$title\" (${PhotogrammetryEngine.formatLength(primaryCm, state.selectedUnit)}) في السجل")
            }
        }
    }

    fun saveClinometerMeasurement(title: String, distanceCm: Double, heightCm: Double) {
        viewModelScope.launch {
            repository.insert(
                MeasurementRecord(
                    title = title.ifBlank { "قياس ارتفاع ومسافة بالزوايا" },
                    primaryLengthCm = heightCm,
                    secondaryLengthCm = distanceCm,
                    calibrationMethod = "تثليث زاوية الميل والارتفاع",
                    referenceDetails = "المسافة الأفقية ${String.format(java.util.Locale.US, "%.1f", distanceCm)} سم • ارتفاع الكاميرا ${String.format(java.util.Locale.US, "%.0f", _uiState.value.cameraHeightCm)} سم",
                    confidencePercent = 94,
                    notes = "الارتفاع العمودي والمسافة الأرضية عبر حساس الميل"
                )
            )
            _uiState.update {
                it.copy(statusBannerMessage = "تم حفظ القياس الثلاثي الأبعاد في السجل")
            }
        }
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    class Factory(
        private val repository: MeasurementRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MeasureViewModel(repository, appContext) as T
        }
    }
}
