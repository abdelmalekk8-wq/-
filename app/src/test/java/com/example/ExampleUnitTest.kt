package com.example

import com.example.domain.CalibrationMode
import com.example.domain.MeasureUnit
import com.example.domain.MeasurementLine
import com.example.domain.NormalizedPoint
import com.example.domain.PhotogrammetryEngine
import com.example.domain.ReferenceObjectPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun referenceObjectCalibration_computesExactRealLengthInCm() {
        // Bank card reference (8.56 cm) spans 0.20 normalized units horizontally
        val refStart = NormalizedPoint(0.10f, 0.50f)
        val refEnd = NormalizedPoint(0.30f, 0.50f)

        // Target object spans 0.60 normalized units horizontally -> should be 3 * 8.56 = 25.68 cm
        val targetLine = MeasurementLine(
            id = 1,
            label = "Target",
            start = NormalizedPoint(0.20f, 0.30f),
            end = NormalizedPoint(0.80f, 0.30f)
        )

        val realCm = PhotogrammetryEngine.calculateRealLengthCm(
            targetLine = targetLine,
            calibrationMode = CalibrationMode.REFERENCE_OBJECT,
            referenceStart = refStart,
            referenceEnd = refEnd,
            referenceRealCm = ReferenceObjectPreset.BANK_CARD_WIDTH.realLengthCm,
            objectDistanceCm = 50.0,
            focalLengthMm = 4.25f,
            sensorWidthMm = 5.60f,
            tiltAngleDeg = 0f,
            applyPerspectiveCorrection = false
        )

        assertEquals(25.68, realCm, 0.05)
    }

    @Test
    fun clinometerRangefinder_computesHorizontalDistanceAndHeight() {
        // Camera at 150 cm height looking 45 degrees down at floor base -> horizontal distance = 150 cm
        val distCm = PhotogrammetryEngine.calculateGroundDistanceFromPitchCm(
            cameraHeightCm = 150.0,
            depressionAngleFromHorizontalDeg = 45.0
        )
        assertEquals(150.0, distCm, 0.1)

        // If top of object is at 0 degrees (eye level), total height = 150 cm
        val heightCm = PhotogrammetryEngine.calculateVerticalObjectHeightCm(
            horizontalDistanceCm = distCm,
            baseDepressionDeg = 45.0,
            topElevationDeg = 0.0
        )
        assertEquals(150.0, heightCm, 0.1)
    }

    @Test
    fun unitFormatting_formatsCorrectlyAcrossUnits() {
        val cmText = PhotogrammetryEngine.formatLength(10.0, MeasureUnit.CM)
        val mmText = PhotogrammetryEngine.formatLength(10.0, MeasureUnit.MM)
        assertTrue(cmText.contains("10.00"))
        assertTrue(mmText.contains("100.0"))
    }
}
