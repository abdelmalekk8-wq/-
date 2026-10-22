package com.example.ui.caliper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensors.DeviceTiltTelemetry
import com.example.ui.StudioUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextSecondary

@Composable
fun ScreenCaliperScreen(
    uiState: StudioUiState,
    liveTilt: DeviceTiltTelemetry,
    onUpdateCaliperMm: (Float) -> Unit,
    onUpdateDpiMultiplier: (Float) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    // Real hardware physical pixels per millimeter along Y axis
    val rawPixelsPerMm = (displayMetrics.ydpi / 25.4f).coerceIn(4.0f, 30.0f)
    val effectivePxPerMm = rawPixelsPerMm * uiState.dpiMultiplier

    val textMeasurer = rememberTextMeasurer()
    val lengthMm = uiState.caliperLengthMm
    val lengthCm = lengthMm / 10f
    val lengthInch = lengthMm / 25.4f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Digital Caliper Readout + 2D Spirit Level
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, PrecisionAmber.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("screen_caliper_card")
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
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = PrecisionAmber
                        )
                        Text(
                            text = "القدمة ذات الورنية ومسطرة الشاشة (1:1)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = LaserCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, LaserCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${displayMetrics.ydpi.toInt()} DPI",
                            style = MaterialTheme.typography.labelMedium,
                            color = LaserCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "ضع أي جسم صغير (عملة، خاتم، برغي، مفتاح، بطاقة) مباشرة فوق شاشة الهاتف وحرّك الفك السفلي ليطابق طوله الحقيقي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", lengthMm)} ملم",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = PrecisionAmber
                        ),
                        modifier = Modifier.testTag("caliper_mm_readout")
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", lengthCm)} سم",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = LaserCyan
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", lengthInch)} إنش",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Quick verification presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "عملة (24 ملم)" to 24.0f,
                        "عرض بطاقة (54 ملم)" to 53.98f,
                        "طول بطاقة (85.6 ملم)" to 85.60f
                    ).forEach { (label, mmVal) ->
                        FilterChip(
                            selected = kotlin.math.abs(lengthMm - mmVal) < 0.5f,
                            onClick = { onUpdateCaliperMm(mmVal) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrecisionAmber.copy(alpha = 0.22f),
                                selectedLabelColor = PrecisionAmber
                            )
                        )
                    }
                }
            }
        }

        // 2. Interactive 1:1 Physical Millimeter & Inch Caliper Canvas
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .pointerInput(effectivePxPerMm) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            val topZeroY = 32f
                            val dyPx = (change.position.y - topZeroY).coerceAtLeast(8f)
                            val newMm = (dyPx / effectivePxPerMm).coerceIn(2f, 120f)
                            onUpdateCaliperMm(newMm)
                        }
                    }
                    .testTag("caliper_interactive_canvas")
            ) {
                val topZeroY = 32f
                val jawY = (topZeroY + lengthMm * effectivePxPerMm).coerceAtMost(size.height - 20f)

                // Shaded measurement zone between top fixed jaw (0 mm) and movable jaw
                drawRect(
                    color = PrecisionAmber.copy(alpha = 0.14f),
                    topLeft = Offset(0f, topZeroY),
                    size = Size(size.width, (jawY - topZeroY).coerceAtLeast(2f))
                )

                // Right edge: Millimeter & Centimeter physical ticks
                val maxMmOnCanvas = ((size.height - topZeroY) / effectivePxPerMm).toInt()
                for (mm in 0..maxMmOnCanvas) {
                    val y = topZeroY + mm * effectivePxPerMm
                    val isCm = mm % 10 == 0
                    val isHalfCm = mm % 5 == 0
                    val tickLen = when {
                        isCm -> 48f
                        isHalfCm -> 32f
                        else -> 18f
                    }
                    drawLine(
                        color = if (isCm) LaserCyan else Color(0xFF94A3B8),
                        start = Offset(size.width - tickLen, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (isCm) 3f else 1.5f
                    )
                    if (isCm) {
                        val cmText = textMeasurer.measure(
                            text = "${mm / 10} سم",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = LaserCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = cmText,
                            topLeft = Offset(size.width - tickLen - cmText.size.width - 8f, y - cmText.size.height / 2f)
                        )
                    }
                }

                // Left edge: Inch physical ticks (1/8 inch subdivisions)
                val pxPerEighthInch = effectivePxPerMm * (25.4f / 8f)
                val maxEighths = ((size.height - topZeroY) / pxPerEighthInch).toInt()
                for (e in 0..maxEighths) {
                    val y = topZeroY + e * pxPerEighthInch
                    val isWholeInch = e % 8 == 0
                    val isHalfInch = e % 4 == 0
                    val tickLen = when {
                        isWholeInch -> 44f
                        isHalfInch -> 28f
                        else -> 15f
                    }
                    drawLine(
                        color = if (isWholeInch) PrecisionAmber else Color(0xFF64748B),
                        start = Offset(0f, y),
                        end = Offset(tickLen, y),
                        strokeWidth = if (isWholeInch) 3f else 1.5f
                    )
                    if (isWholeInch) {
                        val inText = textMeasurer.measure(
                            text = "${e / 8} in",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PrecisionAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = inText,
                            topLeft = Offset(tickLen + 8f, y - inText.size.height / 2f)
                        )
                    }
                }

                // Top Fixed Jaw (0.0 mm)
                drawLine(
                    color = LaserCyan,
                    start = Offset(0f, topZeroY),
                    end = Offset(size.width, topZeroY),
                    strokeWidth = 5f
                )

                // Bottom Movable Caliper Jaw
                drawLine(
                    color = PrecisionAmber,
                    start = Offset(0f, jawY),
                    end = Offset(size.width, jawY),
                    strokeWidth = 6f
                )
                drawRoundRect(
                    color = PrecisionAmber,
                    topLeft = Offset(size.width / 2f - 70f, jawY - 16f),
                    size = Size(140f, 32f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                val jawBadge = textMeasurer.measure(
                    text = "اسحب الفك • ${String.format(java.util.Locale.US, "%.1f", lengthMm)} ملم",
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = ObsidianBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = jawBadge,
                    topLeft = Offset(
                        size.width / 2f - jawBadge.size.width / 2f,
                        jawY - jawBadge.size.height / 2f
                    )
                )
            }
        }

        // 3. Built-in Digital Spirit Level (ميزان الاستواء المائي ثنائي المحور)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = LevelEmerald)
                        Text(
                            text = "ميزان الاستواء الرقمي وضبط دقة الشاشة",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "ميل: ${String.format(java.util.Locale.US, "%.1f°", liveTilt.flatTiltDeg)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (liveTilt.isSteadyAndAligned) LevelEmerald else PrecisionAmber
                    )
                }

                Text(
                    text = "تعديل معامل كثافة الشاشة (DPI) إذا أردت مطابقته مع بطاقة بنكية حقيقية (85.6 ملم):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Slider(
                    value = uiState.dpiMultiplier,
                    onValueChange = onUpdateDpiMultiplier,
                    valueRange = 0.85f..1.15f,
                    colors = SliderDefaults.colors(
                        thumbColor = LaserCyan,
                        activeTrackColor = LaserCyan
                    ),
                    modifier = Modifier.testTag("dpi_multiplier_slider")
                )
            }
        }
    }
}
