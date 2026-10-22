package com.example.ui.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.CalibrationMode
import com.example.domain.MeasureUnit
import com.example.domain.NormalizedPoint
import com.example.domain.PhotogrammetryEngine
import com.example.domain.ReferenceObjectPreset
import com.example.domain.WorkbenchScene
import com.example.sensors.DeviceTiltTelemetry
import com.example.ui.AppScreen
import com.example.ui.StudioUiState
import com.example.ui.studio.WorkbenchCanvasRenderer.drawMagnifierLoupe
import com.example.ui.studio.WorkbenchCanvasRenderer.drawVectorOverlayLine
import com.example.ui.studio.WorkbenchCanvasRenderer.drawWorkbenchBackgroundScene
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

private enum class DragTargetType {
    REF_START,
    REF_END,
    LINE_START,
    LINE_END
}

private data class ActiveDragHandle(
    val type: DragTargetType,
    val lineId: Int = 0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoMeasureStudioScreen(
    uiState: StudioUiState,
    liveTilt: DeviceTiltTelemetry,
    onSelectUnit: (MeasureUnit) -> Unit,
    onSetCalibrationMode: (CalibrationMode) -> Unit,
    onSelectPreset: (ReferenceObjectPreset) -> Unit,
    onUpdateCustomRefCm: (Double) -> Unit,
    onUpdateDistanceCm: (Double) -> Unit,
    onTogglePerspective: (Boolean) -> Unit,
    onUpdateRefPoints: (NormalizedPoint, NormalizedPoint) -> Unit,
    onUpdateLinePoints: (Int, NormalizedPoint, NormalizedPoint) -> Unit,
    onSelectLine: (Int) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    onLoadScene: (WorkbenchScene) -> Unit,
    onOpenLiveCamera: () -> Unit,
    onPhotoPicked: (android.net.Uri, Float) -> Unit,
    onNavigateToClinometer: () -> Unit,
    onSaveMeasurement: (String) -> Unit,
    onDismissBanner: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoPicked(uri, liveTilt.flatTiltDeg)
        }
    }

    val activeLine = uiState.measurementLines.find { it.id == uiState.selectedLineId }
        ?: uiState.measurementLines.first()
    val primaryCm = uiState.computeLineCm(activeLine, 4f / 3f)

    // Calculate secondary line & area if 2+ lines exist
    val secondLine = uiState.measurementLines.firstOrNull { it.id != activeLine.id }
    val secondCm = secondLine?.let { uiState.computeLineCm(it, 4f / 3f) }

    var saveTitleInput by remember(activeLine.id, uiState.activeWorkbenchScene) {
        mutableStateOf(activeLine.label)
    }
    var customRefInput by remember(uiState.customReferenceCm) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", uiState.customReferenceCm))
    }
    var isEditingReferenceNudge by remember { mutableStateOf(false) }
    var nudgeStartEndpoint by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Optional confirmation banner
        AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
            uiState.statusBannerMessage?.let { msg ->
                Surface(
                    color = LevelEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LevelEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "تم",
                                tint = LevelEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Primary Digital Readout Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, PrecisionAmber.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("primary_measurement_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(activeLine.colorHex))
                        )
                        Text(
                            text = "الطول الحقيقي • ${activeLine.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }

                    // Accuracy confidence badge
                    Surface(
                        color = LevelEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, LevelEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "دقة المعايرة ${uiState.confidencePercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = LevelEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Giant digital measurement readout + unit pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = PhotogrammetryEngine.formatNumberOnly(primaryCm, uiState.selectedUnit),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PrecisionAmber
                            ),
                            modifier = Modifier.testTag("digital_length_value")
                        )
                        Text(
                            text = uiState.selectedUnit.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }

                    // Secondary conversions quick strip
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", primaryCm * 10.0)} ملم",
                            style = MaterialTheme.typography.labelMedium,
                            color = LaserCyan
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", primaryCm / 2.54)} إنش",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Unit selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeasureUnit.entries.forEach { unit ->
                        val selected = uiState.selectedUnit == unit
                        Surface(
                            color = if (selected) PrecisionAmber else SlateSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectUnit(unit) }
                                .testTag("unit_chip_${unit.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${unit.arabicName} (${unit.symbol})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ObsidianBg else Color.White
                                )
                            }
                        }
                    }
                }

                // Automatic Area & Perimeter computation when 2+ lines exist
                if (secondCm != null && secondLine != null) {
                    val areaCm2 = primaryCm * secondCm
                    val perimeterCm = 2.0 * (primaryCm + secondCm)
                    Surface(
                        color = SlateSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المساحة (${activeLine.label} × ${secondLine.label}):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = if (areaCm2 >= 10000.0) {
                                    "${String.format(java.util.Locale.US, "%.2f", areaCm2 / 10000.0)} م² • المحيط ${String.format(java.util.Locale.US, "%.1f", perimeterCm)} سم"
                                } else {
                                    "${String.format(java.util.Locale.US, "%.1f", areaCm2)} سم² • المحيط ${String.format(java.util.Locale.US, "%.1f", perimeterCm)} سم"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = LaserCyan
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Photo & Photogrammetry Vector Canvas
        InteractiveMeasurementCanvasCard(
            uiState = uiState,
            onUpdateRefPoints = onUpdateRefPoints,
            onUpdateLinePoints = onUpdateLinePoints
        )

        // 3. Multi-Line Selector + Precision Micro-Nudge Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خطوط القياس على الصورة",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Button(
                        onClick = onAddLine,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrecisionAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_line_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة خط قياس",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة بُعد جديد", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Chips for each measurement line + Reference line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
                        FilterChip(
                            selected = isEditingReferenceNudge,
                            onClick = { isEditingReferenceNudge = true },
                            label = {
                                Text(
                                    "خط المرجع (${String.format(java.util.Locale.US, "%.2f", uiState.effectiveReferenceCm)} سم)"
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(LaserCyan)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserCyan.copy(alpha = 0.22f),
                                selectedLabelColor = LaserCyan
                            )
                        )
                    }

                    uiState.measurementLines.forEach { line ->
                        val isSelected = !isEditingReferenceNudge && line.id == uiState.selectedLineId
                        val cmVal = uiState.computeLineCm(line, 4f / 3f)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isEditingReferenceNudge = false
                                onSelectLine(line.id)
                            },
                            label = {
                                Text(
                                    "${line.label}: ${PhotogrammetryEngine.formatLength(cmVal, uiState.selectedUnit)}"
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(line.colorHex))
                                )
                            },
                            trailingIcon = if (uiState.measurementLines.size > 1) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف الخط",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveLine(line.id) }
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(line.colorHex).copy(alpha = 0.22f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Precision Micro-Nudge Pad (for ultra-exact pixel alignment without finger occlusion)
                Surface(
                    color = SlateSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isEditingReferenceNudge) "تحريك دقيق للمرجع:" else "تحريك دقيق للقياس:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Surface(
                                color = if (nudgeStartEndpoint) LaserCyan.copy(alpha = 0.2f) else PrecisionAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { nudgeStartEndpoint = !nudgeStartEndpoint }
                            ) {
                                Text(
                                    text = if (nudgeStartEndpoint) "نقطة البداية (1)" else "نقطة النهاية (2)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (nudgeStartEndpoint) LaserCyan else PrecisionAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val step = 0.006f
                        val nudgePoint: (Float, Float) -> Unit = { dx, dy ->
                            if (isEditingReferenceNudge) {
                                if (nudgeStartEndpoint) {
                                    onUpdateRefPoints(
                                        NormalizedPoint(uiState.referenceStart.x + dx, uiState.referenceStart.y + dy),
                                        uiState.referenceEnd
                                    )
                                } else {
                                    onUpdateRefPoints(
                                        uiState.referenceStart,
                                        NormalizedPoint(uiState.referenceEnd.x + dx, uiState.referenceEnd.y + dy)
                                    )
                                }
                            } else {
                                if (nudgeStartEndpoint) {
                                    onUpdateLinePoints(
                                        activeLine.id,
                                        NormalizedPoint(activeLine.start.x + dx, activeLine.start.y + dy),
                                        activeLine.end
                                    )
                                } else {
                                    onUpdateLinePoints(
                                        activeLine.id,
                                        activeLine.start,
                                        NormalizedPoint(activeLine.end.x + dx, activeLine.end.y + dy)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { nudgePoint(-step, 0f) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("nudge_left_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "تحريك لليسار", tint = Color.White)
                            }
                            IconButton(
                                onClick = { nudgePoint(step, 0f) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("nudge_right_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "تحريك لليمين", tint = Color.White)
                            }
                            IconButton(
                                onClick = { nudgePoint(0f, -step) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("nudge_up_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "تحريك للأعلى", tint = Color.White)
                            }
                            IconButton(
                                onClick = { nudgePoint(0f, step) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("nudge_down_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تحريك للأسفل", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 4. Photo Source Bar (Live CameraX Capture, Gallery Photo Picker, or Calibrated Workbench Scenes)
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مصدر الصورة المراد قياسها",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenLiveCamera,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "الكاميرا")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصوير بالكاميرا", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.2.dp, LaserCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_gallery_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "المعرض", tint = LaserCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("من المعرض", style = MaterialTheme.typography.labelLarge, color = LaserCyan)
                    }
                }

                Text(
                    text = "أو اختبر الدقة الفورية على مشاهد معيارية بمقياس 1:1 حقيقي:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkbenchScene.entries.forEach { scene ->
                        val active = uiState.capturedPhotoUri == null && uiState.activeWorkbenchScene == scene
                        FilterChip(
                            selected = active,
                            onClick = { onLoadScene(scene) },
                            label = { Text(scene.arabicName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserCyan.copy(alpha = 0.2f),
                                selectedLabelColor = LaserCyan
                            ),
                            modifier = Modifier.testTag("scene_chip_${scene.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 5. Real-World Calibration Engine Card (How We Make the Length Real!)
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, LaserCyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calibration_engine_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "المعايرة الحقيقية",
                        tint = LaserCyan
                    )
                    Column {
                        Text(
                            text = "محرك المعايرة الحقيقية (تحويل البكسل إلى سنتيمتر فعلي)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = uiState.calibrationMode.shortDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Switch between Reference Object Mode and Camera Optics + Distance Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalibrationMode.entries.forEach { mode ->
                        val isSelected = uiState.calibrationMode == mode
                        Surface(
                            color = if (isSelected) LaserCyan else SlateSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetCalibrationMode(mode) }
                                .testTag("calibration_mode_${mode.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = mode.arabicTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) ObsidianBg else Color.White
                                )
                            }
                        }
                    }
                }

                if (uiState.calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
                    Text(
                        text = "اختر الجسم المرجعي الذي وضعت عليه الخط السماوي المتقطع في الصورة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ReferenceObjectPreset.entries.forEach { preset ->
                            val selected = uiState.selectedPreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text("${preset.arabicTitle} (${preset.badgeText})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LaserCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = LaserCyan
                                ),
                                modifier = Modifier.testTag("preset_chip_${preset.id}")
                            )
                        }
                    }

                    if (uiState.selectedPreset == ReferenceObjectPreset.CUSTOM) {
                        OutlinedTextField(
                            value = customRefInput,
                            onValueChange = { text ->
                                customRefInput = text
                                text.toDoubleOrNull()?.let { onUpdateCustomRefCm(it) }
                            },
                            label = { Text("طول المرجع المعروف بالسنتيمتر (سم)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_reference_input")
                        )
                    }
                } else {
                    // Camera Optics + Object Distance Mode
                    Surface(
                        color = SlateSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الخصائص البصرية لعدسة كاميرا جهازك:",
                                style = MaterialTheme.typography.labelLarge,
                                color = LaserCyan
                            )
                            Text(
                                text = "البعد البؤري: ${String.format(java.util.Locale.US, "%.2f", uiState.cameraOptics.focalLengthMm)} ملم • حساس الصورة: ${String.format(java.util.Locale.US, "%.2f×%.2f", uiState.cameraOptics.sensorWidthMm, uiState.cameraOptics.sensorHeightMm)} ملم • زاوية الرؤية: ${String.format(java.util.Locale.US, "%.1f°", uiState.cameraOptics.horizontalFovDeg)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بُعد الكاميرا عن الجسم أثناء التصوير:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", uiState.objectDistanceCm)} سم",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = PrecisionAmber
                        )
                    }

                    Slider(
                        value = uiState.objectDistanceCm.toFloat(),
                        onValueChange = { onUpdateDistanceCm(it.toDouble()) },
                        valueRange = 15f..350f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrecisionAmber,
                            activeTrackColor = PrecisionAmber
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("optics_distance_slider")
                    )

                    OutlinedButton(
                        onClick = onNavigateToClinometer,
                        border = BorderStroke(1.dp, LaserCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = LaserCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حساب المسافة تلقائياً بحساس زاوية الميل",
                            color = LaserCyan,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Perspective Tilt Compensation toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصحيح تشوه المنظور وزاوية الميل (${String.format(java.util.Locale.US, "%.1f°", uiState.capturedTiltDeg)})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = "يعوض رياضياً ميلان الهاتف أثناء التصوير باستخدام حساس الجاذبية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.applyPerspectiveCorrection,
                        onCheckedChange = onTogglePerspective,
                        modifier = Modifier.testTag("perspective_correction_switch")
                    )
                }
            }
        }

        // 6. Save Measurement to History Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حفظ النتيجة في سجل القياسات",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("اسم الشيء المقاس") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("measurement_title_input")
                    )
                    Button(
                        onClick = { onSaveMeasurement(saveTitleInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LevelEmerald,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("save_measurement_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "حفظ")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveMeasurementCanvasCard(
    uiState: StudioUiState,
    onUpdateRefPoints: (NormalizedPoint, NormalizedPoint) -> Unit,
    onUpdateLinePoints: (Int, NormalizedPoint, NormalizedPoint) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var activeDrag by remember { mutableStateOf<ActiveDragHandle?>(null) }

    // Keep latest state references for pointerInput closure
    val currentRefStart = uiState.referenceStart
    val currentRefEnd = uiState.referenceEnd
    val currentLines = uiState.measurementLines

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, SlateCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Instructions header bar inside canvas card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اسحب دوائر الأطراف لتحديد حواف الجسم (تظهر عدسة مكبرة 3.2x تلقائياً)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(
                        uiState.calibrationMode,
                        uiState.measurementLines.size,
                        uiState.activeWorkbenchScene
                    ) {
                        detectDragGestures(
                            onDragStart = { touchOffset ->
                                val w = canvasSize.width.coerceAtLeast(1).toFloat()
                                val h = canvasSize.height.coerceAtLeast(1).toFloat()
                                val nx = touchOffset.x / w
                                val ny = touchOffset.y / h

                                var bestHandle: ActiveDragHandle? = null
                                var bestDist = 0.14 // generous hit radius

                                if (uiState.calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
                                    val dRef1 = hypot(
                                        (nx - uiState.referenceStart.x).toDouble(),
                                        (ny - uiState.referenceStart.y).toDouble()
                                    )
                                    if (dRef1 < bestDist) {
                                        bestDist = dRef1
                                        bestHandle = ActiveDragHandle(DragTargetType.REF_START)
                                    }
                                    val dRef2 = hypot(
                                        (nx - uiState.referenceEnd.x).toDouble(),
                                        (ny - uiState.referenceEnd.y).toDouble()
                                    )
                                    if (dRef2 < bestDist) {
                                        bestDist = dRef2
                                        bestHandle = ActiveDragHandle(DragTargetType.REF_END)
                                    }
                                }

                                uiState.measurementLines.forEach { line ->
                                    val d1 = hypot(
                                        (nx - line.start.x).toDouble(),
                                        (ny - line.start.y).toDouble()
                                    )
                                    if (d1 < bestDist) {
                                        bestDist = d1
                                        bestHandle = ActiveDragHandle(DragTargetType.LINE_START, line.id)
                                    }
                                    val d2 = hypot(
                                        (nx - line.end.x).toDouble(),
                                        (ny - line.end.y).toDouble()
                                    )
                                    if (d2 < bestDist) {
                                        bestDist = d2
                                        bestHandle = ActiveDragHandle(DragTargetType.LINE_END, line.id)
                                    }
                                }

                                activeDrag = bestHandle
                            },
                            onDrag = { change, _ ->
                                val handle = activeDrag ?: return@detectDragGestures
                                change.consume()
                                val w = canvasSize.width.coerceAtLeast(1).toFloat()
                                val h = canvasSize.height.coerceAtLeast(1).toFloat()
                                val newPt = NormalizedPoint(
                                    x = (change.position.x / w).coerceIn(0.03f, 0.97f),
                                    y = (change.position.y / h).coerceIn(0.03f, 0.97f)
                                )

                                when (handle.type) {
                                    DragTargetType.REF_START -> {
                                        onUpdateRefPoints(newPt, uiState.referenceEnd)
                                    }
                                    DragTargetType.REF_END -> {
                                        onUpdateRefPoints(uiState.referenceStart, newPt)
                                    }
                                    DragTargetType.LINE_START -> {
                                        val target = uiState.measurementLines.find { it.id == handle.lineId }
                                        if (target != null) {
                                            onUpdateLinePoints(target.id, newPt, target.end)
                                        }
                                    }
                                    DragTargetType.LINE_END -> {
                                        val target = uiState.measurementLines.find { it.id == handle.lineId }
                                        if (target != null) {
                                            onUpdateLinePoints(target.id, target.start, newPt)
                                        }
                                    }
                                }
                            },
                            onDragEnd = { activeDrag = null },
                            onDragCancel = { activeDrag = null }
                        )
                    }
                    .testTag("photo_measurement_canvas")
            ) {
                // If user captured or picked a photo, render it beneath the vector canvas
                if (uiState.capturedPhotoUri != null) {
                    AsyncImage(
                        model = uiState.capturedPhotoUri,
                        contentDescription = "الصورة الملتقطة للقياس",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (uiState.capturedPhotoUri == null) {
                        drawWorkbenchBackgroundScene(
                            scene = uiState.activeWorkbenchScene,
                            textMeasurer = textMeasurer
                        )
                    }

                    // 1. Draw Reference Object Line (in REFERENCE_OBJECT mode)
                    if (uiState.calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
                        val refText = "${String.format(java.util.Locale.US, "%.2f", uiState.effectiveReferenceCm)} سم"
                        drawVectorOverlayLine(
                            startNorm = currentRefStart,
                            endNorm = currentRefEnd,
                            lineColor = LaserCyan,
                            badgeText = refText,
                            subLabel = "المرجع",
                            isSelected = activeDrag?.type == DragTargetType.REF_START || activeDrag?.type == DragTargetType.REF_END,
                            isDashedReference = true,
                            textMeasurer = textMeasurer
                        )
                    }

                    // 2. Draw all Target Measurement Lines
                    currentLines.forEach { line ->
                        val cm = uiState.computeLineCm(line, size.width / size.height.coerceAtLeast(1f))
                        val formatted = PhotogrammetryEngine.formatLength(cm, uiState.selectedUnit)
                        drawVectorOverlayLine(
                            startNorm = line.start,
                            endNorm = line.end,
                            lineColor = Color(line.colorHex),
                            badgeText = formatted,
                            subLabel = line.label,
                            isSelected = line.id == uiState.selectedLineId,
                            isDashedReference = false,
                            textMeasurer = textMeasurer
                        )
                    }

                    // 3. Draw 3.2x Optical Magnifier Loupe when actively dragging any handle
                    activeDrag?.let { drag ->
                        val (pt, col, readout) = when (drag.type) {
                            DragTargetType.REF_START -> Triple(
                                currentRefStart,
                                LaserCyan,
                                "مرجع ${String.format(java.util.Locale.US, "%.2f", uiState.effectiveReferenceCm)} سم"
                            )
                            DragTargetType.REF_END -> Triple(
                                currentRefEnd,
                                LaserCyan,
                                "مرجع ${String.format(java.util.Locale.US, "%.2f", uiState.effectiveReferenceCm)} سم"
                            )
                            DragTargetType.LINE_START -> {
                                val l = currentLines.find { it.id == drag.lineId } ?: currentLines.first()
                                val cm = uiState.computeLineCm(l, size.width / size.height.coerceAtLeast(1f))
                                Triple(l.start, Color(l.colorHex), PhotogrammetryEngine.formatLength(cm, uiState.selectedUnit))
                            }
                            DragTargetType.LINE_END -> {
                                val l = currentLines.find { it.id == drag.lineId } ?: currentLines.first()
                                val cm = uiState.computeLineCm(l, size.width / size.height.coerceAtLeast(1f))
                                Triple(l.end, Color(l.colorHex), PhotogrammetryEngine.formatLength(cm, uiState.selectedUnit))
                            }
                        }

                        drawMagnifierLoupe(
                            activeNormalizedPoint = pt,
                            activeColor = col,
                            liveReadoutText = readout,
                            scene = if (uiState.capturedPhotoUri == null) uiState.activeWorkbenchScene else null,
                            textMeasurer = textMeasurer
                        )
                    }
                }
            }
        }
    }
}
