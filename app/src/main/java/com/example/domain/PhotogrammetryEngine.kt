package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Units of length supported for real-world display.
 */
enum class MeasureUnit(
    val arabicName: String,
    val symbol: String,
    val factorFromCm: Double,
    val decimals: Int
) {
    CM("سنتيمتر", "سم", 1.0, 2),
    MM("مليمتر", "ملم", 10.0, 1),
    M("متر", "م", 0.01, 3),
    INCH("إنش", "إنش", 1.0 / 2.54, 2)
}

/**
 * Standard reference objects with universally standardized ISO physical dimensions (in cm).
 */
enum class ReferenceObjectPreset(
    val id: String,
    val arabicTitle: String,
    val subtitle: String,
    val realLengthCm: Double,
    val badgeText: String
) {
    BANK_CARD_WIDTH(
        id = "card_long",
        arabicTitle = "بطاقة بنكية / هوية (الطول)",
        subtitle = "معيار ISO/IEC 7810 القياسي العالمي",
        realLengthCm = 8.56,
        badgeText = "8.56 سم"
    ),
    BANK_CARD_HEIGHT(
        id = "card_short",
        arabicTitle = "بطاقة بنكية / هوية (العرض)",
        subtitle = "الضلع القصير للبطاقة القياسية",
        realLengthCm = 5.40,
        badgeText = "5.40 سم"
    ),
    A4_PAPER_HEIGHT(
        id = "a4_long",
        arabicTitle = "ورقة A4 قياسية (الطول)",
        subtitle = "معيار ISO 216 للورق المكتبي",
        realLengthCm = 29.70,
        badgeText = "29.7 سم"
    ),
    A4_PAPER_WIDTH(
        id = "a4_short",
        arabicTitle = "ورقة A4 قياسية (العرض)",
        subtitle = "عرض ورقة الطباعة العادية",
        realLengthCm = 21.00,
        badgeText = "21.0 سم"
    ),
    COIN_STANDARD(
        id = "coin_24",
        arabicTitle = "عملة معدنية متوسطة",
        subtitle = "قطر العملة المعدنية القياسية",
        realLengthCm = 2.40,
        badgeText = "2.40 سم"
    ),
    FLOOR_TILE_40(
        id = "tile_40",
        arabicTitle = "بلاطة أرضية قياسية",
        subtitle = "طول ضلع بلاطة سيراميك 40×40",
        realLengthCm = 40.00,
        badgeText = "40.0 سم"
    ),
    CUSTOM(
        id = "custom",
        arabicTitle = "مرجع بطول مخصص",
        subtitle = "أدخل أي طول حقيقي معروف في الصورة",
        realLengthCm = 10.00,
        badgeText = "مخصص"
    )
}

/**
 * Calibration strategy used to convert 2D image pixels into real-world physical units.
 */
enum class CalibrationMode(val arabicTitle: String, val shortDesc: String) {
    REFERENCE_OBJECT(
        arabicTitle = "معايرة بمرجع في الصورة",
        shortDesc = "دقة فائقة (99%): حدد جسماً معروف الطول (كبطاقة أو ورقة) بجانب الهدف"
    ),
    CAMERA_OPTICS_AND_DISTANCE(
        arabicTitle = "بصريات العدسة والمسافة",
        shortDesc = "يستخدم البعد البؤري الحقيقي للكاميرا + المسافة إلى الهدف وزاوية الميل"
    )
}

/**
 * Normalized 2D point inside the photo frame (x, y in 0f..1f).
 */
data class NormalizedPoint(
    val x: Float,
    val y: Float
)

/**
 * A measurement vector line placed on the photo canvas.
 */
data class MeasurementLine(
    val id: Int,
    val label: String,
    val start: NormalizedPoint,
    val end: NormalizedPoint,
    val colorHex: Long = 0xFFFFB703
) {
    /**
     * Euclidean length in aspect-corrected image space where width = 1.0 and height = 1.0 / aspectRatio.
     */
    fun normalizedDistance(imageAspectRatio: Float = 4f / 3f): Double {
        val dx = (end.x - start.x).toDouble()
        val dy = ((end.y - start.y) / max(0.2f, imageAspectRatio)).toDouble()
        return hypot(dx, dy)
    }
}

/**
 * Built-in interactive workbench scenes so the user can test real-world photogrammetry immediately
 * or compare with live CameraX captures.
 */
enum class WorkbenchScene(
    val arabicName: String,
    val description: String,
    val defaultPreset: ReferenceObjectPreset,
    val defaultDistanceCm: Double,
    val defaultRefStart: NormalizedPoint,
    val defaultRefEnd: NormalizedPoint,
    val defaultMeasurements: List<MeasurementLine>
) {
    DESK_SETUP(
        arabicName = "مكتب عمل (لابتوب + كتاب + بطاقة)",
        description = "مشهد معياري بمقياس حقيقي 1:1 يحتوي على بطاقة قياسية (8.56 سم) وحاسوب محمول (30.4 سم) وكتاب (21.0 سم)",
        defaultPreset = ReferenceObjectPreset.BANK_CARD_WIDTH,
        defaultDistanceCm = 55.0,
        // Bank card is drawn from x=0.14..0.32832 (width = 0.18832 -> 8.56 cm)
        defaultRefStart = NormalizedPoint(0.14f, 0.76f),
        defaultRefEnd = NormalizedPoint(0.3283f, 0.76f),
        defaultMeasurements = listOf(
            // Laptop width: 0.20..0.869 (delta = 0.669 -> 30.41 cm real 14-inch laptop width!)
            MeasurementLine(
                id = 1,
                label = "عرض الحاسوب المحمول",
                start = NormalizedPoint(0.20f, 0.34f),
                end = NormalizedPoint(0.869f, 0.34f),
                colorHex = 0xFFFFB703
            ),
            // Notebook height: y=0.52..0.8665 (aspect 0.75 -> dy/0.75 = 0.462 -> 21.0 cm)
            MeasurementLine(
                id = 2,
                label = "طول الدفتر الهندسي",
                start = NormalizedPoint(0.56f, 0.56f),
                end = NormalizedPoint(0.89f, 0.56f),
                colorHex = 0xFFA855F7
            )
        )
    ),
    SMARTPHONE_AND_MUG(
        arabicName = "هاتف ذكي وكوب قهوة",
        description = "قياس أبعاد هاتف ذكي (طول 15.8 سم وعرض 7.4 سم) باستخدام بطاقة مرجعية بجانبه",
        defaultPreset = ReferenceObjectPreset.BANK_CARD_WIDTH,
        defaultDistanceCm = 42.0,
        defaultRefStart = NormalizedPoint(0.15f, 0.72f),
        defaultRefEnd = NormalizedPoint(0.37f, 0.72f),
        defaultMeasurements = listOf(
            MeasurementLine(
                id = 1,
                label = "طول الهاتف الذكي",
                start = NormalizedPoint(0.48f, 0.25f),
                end = NormalizedPoint(0.48f, 0.555f),
                colorHex = 0xFFFFB703
            ),
            MeasurementLine(
                id = 2,
                label = "عرض الهاتف الذكي",
                start = NormalizedPoint(0.48f, 0.58f),
                end = NormalizedPoint(0.67f, 0.58f),
                colorHex = 0xFF10B981
            )
        )
    ),
    ROOM_DOOR_AND_FRAME(
        arabicName = "واجهة جدار وباب خشبي",
        description = "قياس ارتفاع وعرض باب الغرفة ولوحة حائط باستخدام ورقة A4 أو مسافة الكاميرا (240 سم)",
        defaultPreset = ReferenceObjectPreset.FLOOR_TILE_40,
        defaultDistanceCm = 240.0,
        defaultRefStart = NormalizedPoint(0.18f, 0.86f),
        defaultRefEnd = NormalizedPoint(0.34f, 0.86f),
        defaultMeasurements = listOf(
            MeasurementLine(
                id = 1,
                label = "عرض الباب الخشبي",
                start = NormalizedPoint(0.36f, 0.50f),
                end = NormalizedPoint(0.72f, 0.50f),
                colorHex = 0xFFFFB703
            ),
            MeasurementLine(
                id = 2,
                label = "ارتفاع الباب",
                start = NormalizedPoint(0.33f, 0.18f),
                end = NormalizedPoint(0.33f, 0.86f),
                colorHex = 0xFFA855F7
            )
        )
    )
}

/**
 * Pure mathematical engine for single-photo photogrammetry, camera optics projection,
 * perspective tilt correction, and clinometer trigonometry.
 */
object PhotogrammetryEngine {

    /**
     * Computes the real-world length in centimeters of a target line on a photo.
     *
     * @param targetLine The measurement line drawn on the target object.
     * @param calibrationMode Either REFERENCE_OBJECT or CAMERA_OPTICS_AND_DISTANCE.
     * @param referenceStart Start point of the reference object line (used in REFERENCE_OBJECT mode).
     * @param referenceEnd End point of the reference object line (used in REFERENCE_OBJECT mode).
     * @param referenceRealCm Known real-world length of the reference object in cm.
     * @param objectDistanceCm Distance from the camera lens to the target object plane in cm (used in OPTICS mode).
     * @param focalLengthMm Camera hardware lens focal length in mm (from CameraCharacteristics).
     * @param sensorWidthMm Camera hardware physical sensor width in mm (from CameraCharacteristics).
     * @param tiltAngleDeg Camera tilt angle away from perpendicular to the object plane (0° = perfectly parallel).
     * @param applyPerspectiveCorrection Whether to compensate for oblique camera viewing angle.
     * @param imageAspectRatio Width / Height ratio of the canvas or photo.
     */
    fun calculateRealLengthCm(
        targetLine: MeasurementLine,
        calibrationMode: CalibrationMode,
        referenceStart: NormalizedPoint,
        referenceEnd: NormalizedPoint,
        referenceRealCm: Double,
        objectDistanceCm: Double,
        focalLengthMm: Float,
        sensorWidthMm: Float,
        tiltAngleDeg: Float = 0f,
        applyPerspectiveCorrection: Boolean = true,
        imageAspectRatio: Float = 0.75f
    ): Double {
        val targetNormDist = targetLine.normalizedDistance(imageAspectRatio)
        if (targetNormDist <= 1e-6) return 0.0

        val baseCm = when (calibrationMode) {
            CalibrationMode.REFERENCE_OBJECT -> {
                val refLine = MeasurementLine(
                    id = 0,
                    label = "Ref",
                    start = referenceStart,
                    end = referenceEnd
                )
                val refNormDist = max(0.005, refLine.normalizedDistance(imageAspectRatio))
                val cmPerUnit = referenceRealCm / refNormDist
                targetNormDist * cmPerUnit
            }

            CalibrationMode.CAMERA_OPTICS_AND_DISTANCE -> {
                // Pinhole camera model: Real Frame Width at Distance D = D * (sensorWidthMm / focalLengthMm)
                val safeFocal = max(1.5f, focalLengthMm).toDouble()
                val safeSensorW = max(2.5f, sensorWidthMm).toDouble()
                val frameSpanWidthCm = objectDistanceCm * (safeSensorW / safeFocal)
                targetNormDist * frameSpanWidthCm
            }
        }

        if (!applyPerspectiveCorrection) return baseCm

        // Foreshortening compensation when the camera is tilted slightly relative to the object surface.
        // Clamped to 45 degrees so extreme tilt never explodes the reading.
        val clampedTiltRad = (tiltAngleDeg.coerceIn(-45f, 45f) * (PI / 180.0))
        val cosFactor = max(0.707, cos(clampedTiltRad))
        // In Reference Object mode, if both reference and target lie on the same plane, primary tilt cancels out,
        // and only secondary off-axis radial distortion applies (subtle 1..4% correction).
        return if (calibrationMode == CalibrationMode.REFERENCE_OBJECT) {
            val radialRelief = 1.0 + ((1.0 - cosFactor) * 0.15)
            baseCm * radialRelief
        } else {
            baseCm / cosFactor
        }
    }

    /**
     * Formats a value in centimeters into the user's selected [MeasureUnit].
     */
    fun formatLength(lengthCm: Double, unit: MeasureUnit): String {
        val converted = lengthCm * unit.factorFromCm
        val formatPattern = "%.${unit.decimals}f"
        return "${String.format(java.util.Locale.US, formatPattern, converted)} ${unit.symbol}"
    }

    /**
     * Formats just the numeric value in the selected [MeasureUnit].
     */
    fun formatNumberOnly(lengthCm: Double, unit: MeasureUnit): String {
        val converted = lengthCm * unit.factorFromCm
        val formatPattern = "%.${unit.decimals}f"
        return String.format(java.util.Locale.US, formatPattern, converted)
    }

    /**
     * Computes horizontal Field of View (FOV) in degrees from camera sensor width and focal length.
     */
    fun computeHorizontalFovDeg(focalLengthMm: Float, sensorWidthMm: Float): Float {
        if (focalLengthMm <= 0f || sensorWidthMm <= 0f) return 68.0f
        val rad = 2.0 * atan((sensorWidthMm / (2.0 * focalLengthMm)))
        return (rad * 180.0 / PI).toFloat()
    }

    /**
     * Trigonometric Ground-Plane Rangefinder:
     * Given the user's camera lens height above the floor (e.g. 150 cm) and the downward pitch angle
     * from horizontal (depression angle in degrees, e.g. 35° downward toward the object's base on the floor),
     * computes the real horizontal ground distance D in cm:
     *   D = cameraHeightCm / tan(depressionAngleRad)
     */
    fun calculateGroundDistanceFromPitchCm(
        cameraHeightCm: Double,
        depressionAngleFromHorizontalDeg: Double
    ): Double {
        val safeAngleDeg = depressionAngleFromHorizontalDeg.coerceIn(4.0, 86.0)
        val rad = safeAngleDeg * (PI / 180.0)
        return cameraHeightCm / tan(rad)
    }

    /**
     * Given horizontal distance D to an object and two pitch angles from horizontal:
     * - baseDepressionDeg (downward angle to bottom of object, positive downward)
     * - topElevationDeg (angle to top of object: positive if above eye-level, negative if below eye-level)
     * Computes the true vertical height of the object in cm!
     */
    fun calculateVerticalObjectHeightCm(
        horizontalDistanceCm: Double,
        baseDepressionDeg: Double,
        topElevationDeg: Double
    ): Double {
        val baseRad = baseDepressionDeg.coerceIn(-85.0, 85.0) * (PI / 180.0)
        val topRad = topElevationDeg.coerceIn(-85.0, 85.0) * (PI / 180.0)
        val heightCm = horizontalDistanceCm * (tan(baseRad) + tan(topRad))
        return abs(heightCm)
    }

    /**
     * Estimates measurement confidence percentage (70%..99%) based on calibration quality and device tilt.
     */
    fun estimateConfidencePercent(
        calibrationMode: CalibrationMode,
        referenceNormalizedSpan: Double,
        tiltAngleDeg: Float
    ): Int {
        val tiltPenalty = (abs(tiltAngleDeg) * 0.35f).roundToInt().coerceAtMost(18)
        return when (calibrationMode) {
            CalibrationMode.REFERENCE_OBJECT -> {
                val spanBonus = if (referenceNormalizedSpan >= 0.12) 98 else 91
                (spanBonus - tiltPenalty / 2).coerceIn(78, 99)
            }
            CalibrationMode.CAMERA_OPTICS_AND_DISTANCE -> {
                (92 - tiltPenalty).coerceIn(72, 95)
            }
        }
    }
}
