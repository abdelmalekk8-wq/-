package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.domain.PhotogrammetryEngine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Real optical specifications queried directly from the Android Camera2 Hardware HAL.
 */
data class CameraOpticalSpecs(
    val focalLengthMm: Float = 4.25f,
    val sensorWidthMm: Float = 5.60f,
    val sensorHeightMm: Float = 4.20f,
    val horizontalFovDeg: Float = 66.8f,
    val isHardwareReported: Boolean = false
)

/**
 * Real-time 3D orientation & tilt telemetry from the device's accelerometer / gravity sensors.
 */
data class DeviceTiltTelemetry(
    /** Angle relative to a flat tabletop (0° = phone lying flat / parallel to desk). */
    val flatTiltDeg: Float = 0f,
    /** Downward/upward pitch angle when holding phone vertically viewing a scene (0° = looking straight ahead at horizon, +30° = looking 30° down at floor). */
    val horizonDepressionDeg: Float = 25f,
    /** Side-to-side roll angle in degrees (0° = level horizontally). */
    val rollDeg: Float = 0f,
    /** Whether the phone is well-aligned (either flat for top-down shots or level horizontally). */
    val isSteadyAndAligned: Boolean = true,
    /** Whether a physical accelerometer sensor is active on this device. */
    val hasHardwareSensor: Boolean = true
)

object DeviceOpticsAndSensors {

    /**
     * Queries Android's CameraManager for the back camera's actual physical focal length (mm)
     * and physical sensor size (mm) to compute the true optical Field of View (FOV).
     */
    fun readBackCameraOptics(context: Context): CameraOpticalSpecs {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return CameraOpticalSpecs()

            for (cameraId in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK || cameraManager.cameraIdList.size == 1) {
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val f = focalLengths?.firstOrNull()?.takeIf { it > 0.5f } ?: 4.25f
                    val sw = sensorSize?.width?.takeIf { it > 1.0f } ?: 5.60f
                    val sh = sensorSize?.height?.takeIf { it > 1.0f } ?: 4.20f
                    val fov = PhotogrammetryEngine.computeHorizontalFovDeg(f, sw)
                    return CameraOpticalSpecs(
                        focalLengthMm = f,
                        sensorWidthMm = sw,
                        sensorHeightMm = sh,
                        horizontalFovDeg = fov,
                        isHardwareReported = focalLengths != null && sensorSize != null
                    )
                }
            }
            CameraOpticalSpecs()
        } catch (_: Exception) {
            CameraOpticalSpecs()
        }
    }

    /**
     * Streams live 3D accelerometer telemetry for both top-down desk alignment and vertical clinometer rangefinding.
     */
    fun observeDeviceTilt(context: Context): Flow<DeviceTiltTelemetry> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            trySend(
                DeviceTiltTelemetry(
                    flatTiltDeg = 1.2f,
                    horizonDepressionDeg = 28.0f,
                    rollDeg = 0.4f,
                    isSteadyAndAligned = true,
                    hasHardwareSensor = false
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        var smoothGx = 0f
        var smoothGy = 0f
        var smoothGz = 9.81f
        val alpha = 0.18f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size < 3) return

                smoothGx += alpha * (values[0] - smoothGx)
                smoothGy += alpha * (values[1] - smoothGy)
                smoothGz += alpha * (values[2] - smoothGz)

                val gNorm = hypot(hypot(smoothGx.toDouble(), smoothGy.toDouble()), smoothGz.toDouble())
                    .coerceAtLeast(0.1)

                // 1. Flat tabletop tilt: 0° when phone is parallel to ground (Gz ≈ ±gNorm)
                val xyMag = hypot(smoothGx.toDouble(), smoothGy.toDouble())
                val flatTilt = (atan2(xyMag, abs(smoothGz.toDouble())) * 180.0 / PI).toFloat()

                // 2. Horizon depression angle when holding phone upright in portrait:
                // When upright looking straight at horizon, Gy ≈ 9.8, Gz ≈ 0 -> angle = 0°.
                // When tilting phone downward toward floor, Gz becomes positive -> positive depression angle.
                val depressionDeg = (atan2(smoothGz.toDouble(), smoothGy.toDouble()) * 180.0 / PI).toFloat()

                // 3. Roll angle (sideways tilt)
                val roll = (atan2(smoothGx.toDouble(), hypot(smoothGy.toDouble(), smoothGz.toDouble())) * 180.0 / PI).toFloat()

                val isAligned = flatTilt < 6.0f || abs(roll) < 4.0f

                trySend(
                    DeviceTiltTelemetry(
                        flatTiltDeg = flatTilt,
                        horizonDepressionDeg = depressionDeg,
                        rollDeg = roll,
                        isSteadyAndAligned = isAligned,
                        hasHardwareSensor = true
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // Emit an immediate initial reading before sensor events arrive
        trySend(DeviceTiltTelemetry())

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
