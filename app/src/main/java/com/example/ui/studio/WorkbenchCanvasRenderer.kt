package com.example.ui.studio

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.domain.CalibrationMode
import com.example.domain.MeasureUnit
import com.example.domain.MeasurementLine
import com.example.domain.NormalizedPoint
import com.example.domain.PhotogrammetryEngine
import com.example.domain.WorkbenchScene
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.ObsidianBg
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the calibrated workbench scenes, measurement vectors, arrowheads, labels,
 * and the 3.5x precision optical magnifier loupe when dragging endpoints.
 */
object WorkbenchCanvasRenderer {

    fun DrawScope.drawWorkbenchBackgroundScene(
        scene: WorkbenchScene,
        textMeasurer: TextMeasurer
    ) {
        val w = size.width
        val h = size.height

        // Dark engineering surface background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF141E33), Color(0xFF0D1526))
            )
        )

        // Subtle millimeter/centimeter photogrammetry grid
        val gridStep = w / 16f
        for (i in 1..15) {
            val x = i * gridStep
            drawLine(
                color = Color(0xFF1E2D4A).copy(alpha = 0.55f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = if (i % 4 == 0) 1.6f else 0.8f
            )
        }
        val vSteps = (h / gridStep).toInt()
        for (j in 1..vSteps) {
            val y = j * gridStep
            drawLine(
                color = Color(0xFF1E2D4A).copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (j % 4 == 0) 1.6f else 0.8f
            )
        }

        when (scene) {
            WorkbenchScene.DESK_SETUP -> drawDeskSetupObjects(w, h, textMeasurer)
            WorkbenchScene.SMARTPHONE_AND_MUG -> drawSmartphoneAndMugObjects(w, h, textMeasurer)
            WorkbenchScene.ROOM_DOOR_AND_FRAME -> drawRoomDoorScene(w, h, textMeasurer)
        }
    }

    private fun DrawScope.drawDeskSetupObjects(w: Float, h: Float, textMeasurer: TextMeasurer) {
        // 1. 14-inch Laptop top-down body (x: 0.20..0.869 -> 30.41 cm)
        val lapX = w * 0.20f
        val lapY = h * 0.12f
        val lapW = w * 0.669f
        val lapH = h * 0.34f

        // Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(lapX + 6f, lapY + 8f),
            size = Size(lapW, lapH),
            cornerRadius = CornerRadius(18f, 18f)
        )
        // Aluminum chassis
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF1E293B)),
                start = Offset(lapX, lapY),
                end = Offset(lapX + lapW, lapY + lapH)
            ),
            topLeft = Offset(lapX, lapY),
            size = Size(lapW, lapH),
            cornerRadius = CornerRadius(16f, 16f)
        )
        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(lapX, lapY),
            size = Size(lapW, lapH),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 2.5f)
        )
        // Keyboard well
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(lapX + lapW * 0.08f, lapY + lapH * 0.14f),
            size = Size(lapW * 0.84f, lapH * 0.48f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Trackpad
        drawRoundRect(
            color = Color(0xFF26354D),
            topLeft = Offset(lapX + lapW * 0.35f, lapY + lapH * 0.68f),
            size = Size(lapW * 0.30f, lapH * 0.24f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // 2. ISO ID-1 Standard Bank Card (x: 0.14..0.3283 -> 8.56 cm)
        val cardX = w * 0.14f
        val cardY = h * 0.66f
        val cardW = w * 0.1883f
        val cardH = cardW * (53.98f / 85.60f)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(cardX + 4f, cardY + 5f),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                start = Offset(cardX, cardY),
                end = Offset(cardX + cardW, cardY + cardH)
            ),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f)
        )
        // Gold EMV chip on card
        drawRoundRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(cardX + cardW * 0.12f, cardY + cardH * 0.36f),
            size = Size(cardW * 0.20f, cardH * 0.26f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawObjectSubtleCaption(
            textMeasurer = textMeasurer,
            text = "بطاقة قياسية ISO",
            topLeft = Offset(cardX + 6f, cardY + cardH * 0.68f)
        )

        // 3. Engineering Hardcover Notebook (x: 0.56..0.89)
        val bookX = w * 0.56f
        val bookY = h * 0.52f
        val bookW = w * 0.33f
        val bookH = h * 0.36f

        drawRoundRect(
            color = Color(0xFF312E81),
            topLeft = Offset(bookX, bookY),
            size = Size(bookW, bookH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFF818CF8),
            topLeft = Offset(bookX, bookY),
            size = Size(bookW, bookH),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f)
        )
        // Spine band
        drawRect(
            color = Color(0xFF1E1B4B),
            topLeft = Offset(bookX, bookY),
            size = Size(bookW * 0.12f, bookH)
        )
        drawObjectSubtleCaption(
            textMeasurer = textMeasurer,
            text = "دفتر هندسي A5",
            topLeft = Offset(bookX + bookW * 0.22f, bookY + bookH * 0.42f)
        )
    }

    private fun DrawScope.drawSmartphoneAndMugObjects(w: Float, h: Float, textMeasurer: TextMeasurer) {
        // Reference Bank Card on left (x: 0.15..0.37)
        val cardX = w * 0.15f
        val cardY = h * 0.60f
        val cardW = w * 0.22f
        val cardH = cardW * (53.98f / 85.60f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0D9488), Color(0xFF115E59)),
                start = Offset(cardX, cardY),
                end = Offset(cardX + cardW, cardY + cardH)
            ),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFF2DD4BF),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f)
        )
        drawRoundRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(cardX + cardW * 0.12f, cardY + cardH * 0.35f),
            size = Size(cardW * 0.18f, cardH * 0.25f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawObjectSubtleCaption(
            textMeasurer = textMeasurer,
            text = "مرجع 8.56 سم",
            topLeft = Offset(cardX + 8f, cardY + cardH * 0.66f)
        )

        // Smartphone (x: 0.48..0.67, y: 0.25..0.555)
        val phoneX = w * 0.48f
        val phoneY = h * 0.25f
        val phoneW = w * 0.19f
        val phoneH = h * 0.305f

        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(22f, 22f)
        )
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(22f, 22f),
            style = Stroke(width = 3f)
        )
        // Screen bezel
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(phoneX + 6f, phoneY + 8f),
            size = Size(phoneW - 12f, phoneH - 16f),
            cornerRadius = CornerRadius(16f, 16f)
        )
        // Ceramic Mug (top-down circle on top-left)
        val mugCenter = Offset(w * 0.26f, h * 0.34f)
        val mugRadius = w * 0.105f
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = mugRadius,
            center = mugCenter
        )
        drawCircle(
            color = Color(0xFF78350F),
            radius = mugRadius * 0.82f,
            center = mugCenter
        )
    }

    private fun DrawScope.drawRoomDoorScene(w: Float, h: Float, textMeasurer: TextMeasurer) {
        // Floor line at y = 0.86
        val floorY = h * 0.86f
        drawLine(
            color = Color(0xFF475569),
            start = Offset(0f, floorY),
            end = Offset(w, floorY),
            strokeWidth = 3f
        )
        // Reference 40 cm floor tile at x = 0.18..0.34
        val tileX = w * 0.18f
        val tileW = w * 0.16f
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(tileX, floorY),
            size = Size(tileW, h - floorY)
        )
        drawRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(tileX, floorY),
            size = Size(tileW, h - floorY),
            style = Stroke(width = 2f)
        )
        drawObjectSubtleCaption(
            textMeasurer = textMeasurer,
            text = "بلاطة 40 سم",
            topLeft = Offset(tileX + 6f, floorY + 8f)
        )

        // Wooden Door Frame (x: 0.36..0.72, y: 0.18..0.86)
        val doorX = w * 0.36f
        val doorY = h * 0.18f
        val doorW = w * 0.36f
        val doorH = floorY - doorY

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF78350F), Color(0xFF451A03))
            ),
            topLeft = Offset(doorX, doorY),
            size = Size(doorW, doorH)
        )
        drawRect(
            color = Color(0xFFD97706),
            topLeft = Offset(doorX, doorY),
            size = Size(doorW, doorH),
            style = Stroke(width = 3.5f)
        )
        // Door handle
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = 8f,
            center = Offset(doorX + doorW * 0.85f, doorY + doorH * 0.52f)
        )
    }

    private fun DrawScope.drawObjectSubtleCaption(
        textMeasurer: TextMeasurer,
        text: String,
        topLeft: Offset
    ) {
        val result = textMeasurer.measure(
            text = text,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(result, topLeft = topLeft)
    }

    /**
     * Draws a measurement or reference vector line with engineering end-ticks,
     * draggable target rings, and a pill badge displaying the live physical measurement.
     */
    fun DrawScope.drawVectorOverlayLine(
        startNorm: NormalizedPoint,
        endNorm: NormalizedPoint,
        lineColor: Color,
        badgeText: String,
        subLabel: String,
        isSelected: Boolean,
        isDashedReference: Boolean,
        textMeasurer: TextMeasurer
    ) {
        val p1 = Offset(startNorm.x * size.width, startNorm.y * size.height)
        val p2 = Offset(endNorm.x * size.width, endNorm.y * size.height)

        val angleRad = atan2(p2.y - p1.y, p2.x - p1.x)
        val perpX = -sin(angleRad) * 14f
        val perpY = cos(angleRad) * 14f

        // Glow under line when selected
        if (isSelected) {
            drawLine(
                color = lineColor.copy(alpha = 0.28f),
                start = p1,
                end = p2,
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )
        }

        // Main vector line
        drawLine(
            color = lineColor,
            start = p1,
            end = p2,
            strokeWidth = if (isSelected) 5.5f else 4f,
            cap = StrokeCap.Round,
            pathEffect = if (isDashedReference) {
                PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
            } else {
                null
            }
        )

        // Engineering caliper perpendicular end-ticks
        drawLine(
            color = lineColor,
            start = Offset(p1.x - perpX, p1.y - perpY),
            end = Offset(p1.x + perpX, p1.y + perpY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = lineColor,
            start = Offset(p2.x - perpX, p2.y - perpY),
            end = Offset(p2.x + perpX, p2.y + perpY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )

        // Draggable endpoint reticle handles
        listOf(p1, p2).forEach { pt ->
            drawCircle(
                color = ObsidianBg.copy(alpha = 0.85f),
                radius = if (isSelected) 18f else 14f,
                center = pt
            )
            drawCircle(
                color = lineColor,
                radius = if (isSelected) 18f else 14f,
                center = pt,
                style = Stroke(width = if (isSelected) 4f else 2.8f)
            )
            drawCircle(
                color = Color.White,
                radius = 4.5f,
                center = pt
            )
        }

        // Pill label at midpoint
        val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        val combinedText = if (subLabel.isNotEmpty()) "$subLabel: $badgeText" else badgeText
        val measured = textMeasurer.measure(
            text = combinedText,
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val padH = 16f
        val padV = 8f
        val pillW = measured.size.width + padH * 2
        val pillH = measured.size.height + padV * 2
        val pillTopLeft = Offset(
            x = (mid.x - pillW / 2f).coerceIn(8f, (size.width - pillW - 8f).coerceAtLeast(8f)),
            y = (mid.y - pillH - 14f).coerceIn(8f, (size.height - pillH - 8f).coerceAtLeast(8f))
        )

        drawRoundRect(
            color = ObsidianBg.copy(alpha = 0.92f),
            topLeft = pillTopLeft,
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = lineColor,
            topLeft = pillTopLeft,
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = if (isSelected) 2.6f else 1.5f)
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(pillTopLeft.x + padH, pillTopLeft.y + padV)
        )
    }

    /**
     * Draws a 3.5x magnified precision reticle loupe in the upper corner when the user is actively
     * dragging any endpoint so they can align with exact object edges.
     */
    fun DrawScope.drawMagnifierLoupe(
        activeNormalizedPoint: NormalizedPoint,
        activeColor: Color,
        liveReadoutText: String,
        scene: WorkbenchScene?,
        textMeasurer: TextMeasurer
    ) {
        val loupeRadius = 68f
        // Place loupe in top-left or top-right away from the user's finger
        val loupeCenter = if (activeNormalizedPoint.x < 0.45f && activeNormalizedPoint.y < 0.45f) {
            Offset(size.width - loupeRadius - 20f, loupeRadius + 20f)
        } else {
            Offset(loupeRadius + 20f, loupeRadius + 20f)
        }

        val targetPx = Offset(
            activeNormalizedPoint.x * size.width,
            activeNormalizedPoint.y * size.height
        )

        val circlePath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center = loupeCenter,
                    radius = loupeRadius
                )
            )
        }

        // Outer shadow ring
        drawCircle(
            color = ObsidianBg.copy(alpha = 0.95f),
            radius = loupeRadius + 6f,
            center = loupeCenter
        )

        clipPath(circlePath) {
            drawRect(
                color = Color(0xFF0E1729),
                topLeft = Offset(loupeCenter.x - loupeRadius, loupeCenter.y - loupeRadius),
                size = Size(loupeRadius * 2, loupeRadius * 2)
            )
            // Zoomed 3.2x transform around targetPx
            val zoom = 3.2f
            withTransform({
                translate(
                    left = loupeCenter.x - targetPx.x * zoom,
                    top = loupeCenter.y - targetPx.y * zoom
                )
                scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
            }) {
                if (scene != null) {
                    drawWorkbenchBackgroundScene(scene, textMeasurer)
                }
            }

            // Precision sub-millimeter crosshair reticle inside loupe
            drawLine(
                color = activeColor.copy(alpha = 0.85f),
                start = Offset(loupeCenter.x - loupeRadius, loupeCenter.y),
                end = Offset(loupeCenter.x + loupeRadius, loupeCenter.y),
                strokeWidth = 1.8f
            )
            drawLine(
                color = activeColor.copy(alpha = 0.85f),
                start = Offset(loupeCenter.x, loupeCenter.y - loupeRadius),
                end = Offset(loupeCenter.x, loupeCenter.y + loupeRadius),
                strokeWidth = 1.8f
            )
            drawCircle(
                color = activeColor,
                radius = 12f,
                center = loupeCenter,
                style = Stroke(width = 2f)
            )
        }

        // Loupe bezel ring
        drawCircle(
            color = activeColor,
            radius = loupeRadius,
            center = loupeCenter,
            style = Stroke(width = 3.5f)
        )

        // Readout badge below loupe
        val labelResult = textMeasurer.measure(
            text = "3.2x • $liveReadoutText",
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                color = LaserCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val badgeW = labelResult.size.width + 20f
        val badgeH = labelResult.size.height + 8f
        val badgeTopLeft = Offset(
            loupeCenter.x - badgeW / 2f,
            loupeCenter.y + loupeRadius + 8f
        )
        drawRoundRect(
            color = ObsidianBg.copy(alpha = 0.94f),
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = activeColor,
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 1.5f)
        )
        drawText(
            textLayoutResult = labelResult,
            topLeft = Offset(badgeTopLeft.x + 10f, badgeTopLeft.y + 4f)
        )
    }
}
