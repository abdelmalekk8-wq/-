package com.example.ui.clinometer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MeasureUnit
import com.example.domain.PhotogrammetryEngine
import com.example.sensors.DeviceTiltTelemetry
import com.example.ui.StudioUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.MeasurePurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextSecondary

@Composable
fun ClinometerRangefinderScreen(
    uiState: StudioUiState,
    liveTilt: DeviceTiltTelemetry,
    onUpdateCameraHeight: (Double) -> Unit,
    onUpdateBaseAngle: (Double) -> Unit,
    onUpdateTopAngle: (Double) -> Unit,
    onApplyDistanceToStudio: (Double) -> Unit,
    onSaveClinometerMeasurement: (String, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val horizontalDistanceCm = PhotogrammetryEngine.calculateGroundDistanceFromPitchCm(
        cameraHeightCm = uiState.cameraHeightCm,
        depressionAngleFromHorizontalDeg = uiState.lockedBaseDepressionDeg
    )
    val verticalHeightCm = PhotogrammetryEngine.calculateVerticalObjectHeightCm(
        horizontalDistanceCm = horizontalDistanceCm,
        baseDepressionDeg = uiState.lockedBaseDepressionDeg,
        topElevationDeg = uiState.lockedTopElevationDeg
    )

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dual Digital Readout: Ground Distance + Object Height
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, LaserCyan.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clinometer_summary_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "مقياس التثليث الهندسي للأجسام الكبيرة والمسافات",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "يقيس المسافة الأفقية والارتفاع الحقيقي لأي جسم على الأرض (باب، جدار، خزانة، طاولة) باستخدام ارتفاع الهاتف وزاوية الميل",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Distance box
                    Surface(
                        color = SlateSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LaserCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "المسافة الأفقية (D)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = PhotogrammetryEngine.formatLength(horizontalDistanceCm, uiState.selectedUnit),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = LaserCyan
                                ),
                                modifier = Modifier.testTag("clinometer_distance_value")
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", horizontalDistanceCm / 100.0)} متر",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Object Height box
                    Surface(
                        color = SlateSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrecisionAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "الارتفاع الحقيقي (H)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = PhotogrammetryEngine.formatLength(verticalHeightCm, uiState.selectedUnit),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = PrecisionAmber
                                ),
                                modifier = Modifier.testTag("clinometer_height_value")
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", verticalHeightCm / 100.0)} متر",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 2. Live Geometric Ray-Tracing Diagram Canvas
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBg),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "المخطط البصري المباشر (تثليث أشعة الكاميرا)",
                    style = MaterialTheme.typography.labelLarge,
                    color = LaserCyan,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val floorY = h * 0.84f
                    val phoneX = w * 0.16f
                    val phoneY = h * 0.44f
                    val targetX = w * 0.80f

                    // Compute scaled visual top of target object
                    val heightRatio = (verticalHeightCm / uiState.cameraHeightCm.coerceAtLeast(10.0))
                        .coerceIn(0.25, 1.85)
                        .toFloat()
                    val camPixelH = floorY - phoneY
                    val targetTopY = (floorY - camPixelH * heightRatio).coerceIn(18f, floorY - 20f)

                    // Ground line
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(w * 0.06f, floorY),
                        end = Offset(w * 0.94f, floorY),
                        strokeWidth = 4f
                    )

                    // Eye-level horizontal dashed reference ray
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(phoneX, phoneY),
                        end = Offset(targetX, phoneY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Shaded triangle area
                    val triPath = Path().apply {
                        moveTo(phoneX, phoneY)
                        lineTo(targetX, floorY)
                        lineTo(targetX, targetTopY)
                        close()
                    }
                    drawPath(
                        path = triPath,
                        color = LaserCyan.copy(alpha = 0.12f)
                    )

                    // Camera height vertical post (Phone to floor)
                    drawLine(
                        color = LevelEmerald,
                        start = Offset(phoneX, floorY),
                        end = Offset(phoneX, phoneY),
                        strokeWidth = 5f
                    )

                    // Ray to Base
                    drawLine(
                        color = LaserCyan,
                        start = Offset(phoneX, phoneY),
                        end = Offset(targetX, floorY),
                        strokeWidth = 3.5f
                    )

                    // Ray to Top
                    drawLine(
                        color = MeasurePurple,
                        start = Offset(phoneX, phoneY),
                        end = Offset(targetX, targetTopY),
                        strokeWidth = 3.5f
                    )

                    // Target Object Vertical Pillar
                    drawLine(
                        color = PrecisionAmber,
                        start = Offset(targetX, floorY),
                        end = Offset(targetX, targetTopY),
                        strokeWidth = 8f
                    )

                    // Phone node & Target nodes
                    drawCircle(color = LevelEmerald, radius = 9f, center = Offset(phoneX, phoneY))
                    drawCircle(color = LaserCyan, radius = 8f, center = Offset(targetX, floorY))
                    drawCircle(color = PrecisionAmber, radius = 8f, center = Offset(targetX, targetTopY))

                    // Labels
                    val camLabel = textMeasurer.measure(
                        text = "الكاميرا ${uiState.cameraHeightCm.toInt()} سم",
                        style = TextStyle(color = LevelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(camLabel, topLeft = Offset(phoneX - 24f, phoneY - 26f))

                    val distLabel = textMeasurer.measure(
                        text = "المسافة D = ${String.format(java.util.Locale.US, "%.0f", horizontalDistanceCm)} سم",
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = LaserCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(distLabel, topLeft = Offset((phoneX + targetX) / 2f - 45f, floorY + 6f))

                    val objLabel = textMeasurer.measure(
                        text = "${String.format(java.util.Locale.US, "%.1f", verticalHeightCm)} سم",
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = PrecisionAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(objLabel, topLeft = Offset(targetX - objLabel.size.width - 12f, (floorY + targetTopY) / 2f - 10f))
                }
            }
        }

        // 3. Step 1: Camera Lens Height Above Floor
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "١. ارتفاع عدسة الهاتف عن الأرض:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${uiState.cameraHeightCm.toInt()} سم",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = LevelEmerald
                    )
                }

                // Quick posture presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "على طاولة (75 سم)" to 75.0,
                        "جالس (115 سم)" to 115.0,
                        "واقف (150 سم)" to 150.0,
                        "مرتفع (165 سم)" to 165.0
                    ).forEach { (label, cmVal) ->
                        FilterChip(
                            selected = uiState.cameraHeightCm.toInt() == cmVal.toInt(),
                            onClick = { onUpdateCameraHeight(cmVal) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LevelEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = LevelEmerald
                            )
                        )
                    }
                }

                Slider(
                    value = uiState.cameraHeightCm.toFloat(),
                    onValueChange = { onUpdateCameraHeight(it.toDouble()) },
                    valueRange = 40f..210f,
                    colors = SliderDefaults.colors(
                        thumbColor = LevelEmerald,
                        activeTrackColor = LevelEmerald
                    ),
                    modifier = Modifier.testTag("camera_height_slider")
                )
            }
        }

        // 4. Step 2 & 3: Base Angle & Top Angle Controls + Live Sensor Lock
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live accelerometer sensor bar
                Surface(
                    color = SlateSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                tint = LaserCyan
                            )
                            Text(
                                text = "زاوية حساس الهاتف الحية الآن:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f°", liveTilt.horizonDepressionDeg)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = LaserCyan
                        )
                    }
                }

                // Base depression angle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "٢. زاوية الميل نحو قاعدة الجسم على الأرض:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "كلما صغرت الزاوية زادت المسافة الأفقية المحسوبة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { onUpdateBaseAngle(kotlin.math.abs(liveTilt.horizonDepressionDeg.toDouble())) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("lock_base_angle_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${String.format(java.util.Locale.US, "%.1f°", uiState.lockedBaseDepressionDeg)}")
                    }
                }

                Slider(
                    value = uiState.lockedBaseDepressionDeg.toFloat(),
                    onValueChange = { onUpdateBaseAngle(it.toDouble()) },
                    valueRange = 8f..75f,
                    colors = SliderDefaults.colors(
                        thumbColor = LaserCyan,
                        activeTrackColor = LaserCyan
                    ),
                    modifier = Modifier.testTag("base_angle_slider")
                )

                // Top elevation angle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "٣. زاوية الميل نحو قمة الجسم:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "موجبة إذا كانت القمة أعلى من مستوى الكاميرا، وسالبة إذا كانت أدنى",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { onUpdateTopAngle(-liveTilt.horizonDepressionDeg.toDouble()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrecisionAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("lock_top_angle_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${String.format(java.util.Locale.US, "%.1f°", uiState.lockedTopElevationDeg)}")
                    }
                }

                Slider(
                    value = uiState.lockedTopElevationDeg.toFloat(),
                    onValueChange = { onUpdateTopAngle(it.toDouble()) },
                    valueRange = -25f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrecisionAmber,
                        activeTrackColor = PrecisionAmber
                    ),
                    modifier = Modifier.testTag("top_angle_slider")
                )

                // Action buttons: Apply distance to Photo Studio OR Save to History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onApplyDistanceToStudio(horizontalDistanceCm) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_distance_to_studio_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال المسافة لاستوديو الصور", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            onSaveClinometerMeasurement(
                                "قياس ارتفاع ومسافة بالزوايا",
                                horizontalDistanceCm,
                                verticalHeightCm
                            )
                        },
                        border = BorderStroke(1.2.dp, LevelEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_clinometer_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = LevelEmerald)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ", color = LevelEmerald, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
