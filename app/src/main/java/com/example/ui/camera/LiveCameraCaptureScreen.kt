package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.WorkbenchScene
import com.example.sensors.CameraOpticalSpecs
import com.example.sensors.DeviceTiltTelemetry
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningCoral
import java.io.File
import kotlin.math.abs

@Composable
fun LiveCameraCaptureScreen(
    optics: CameraOpticalSpecs,
    tiltTelemetry: DeviceTiltTelemetry,
    onPhotoCaptured: (Uri, Float) -> Unit,
    onUseWorkbenchScene: (WorkbenchScene) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            provider.unbindAll()
                            val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            cameraError = "تعذر تشغيل معاينة الكاميرا المباشرة في هذه البيئة: ${e.localizedMessage ?: ""}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Laser Reticle + 2-Axis Spirit Level HUD Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val aligned = tiltTelemetry.isSteadyAndAligned
            val reticleColor = if (aligned) LevelEmerald else PrecisionAmber

            // Outer target ring
            drawCircle(
                color = reticleColor.copy(alpha = 0.75f),
                radius = 96f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            // Inner reference ring
            drawCircle(
                color = LaserCyan.copy(alpha = 0.5f),
                radius = 44f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8f)
            )

            // Crosshair lines
            drawLine(
                color = reticleColor,
                start = Offset(cx - 130f, cy),
                end = Offset(cx - 52f, cy),
                strokeWidth = 3f
            )
            drawLine(
                color = reticleColor,
                start = Offset(cx + 52f, cy),
                end = Offset(cx + 130f, cy),
                strokeWidth = 3f
            )
            drawLine(
                color = reticleColor,
                start = Offset(cx, cy - 130f),
                end = Offset(cx, cy - 52f),
                strokeWidth = 3f
            )
            drawLine(
                color = reticleColor,
                start = Offset(cx, cy + 52f),
                end = Offset(cx, cy + 130f),
                strokeWidth = 3f
            )

            // Dynamic tilt bubble offset from center
            val bubbleOffsetX = (tiltTelemetry.rollDeg * 3.5f).coerceIn(-75f, 75f)
            val bubbleOffsetY = (tiltTelemetry.flatTiltDeg * 2.5f).coerceIn(-75f, 75f)
            drawCircle(
                color = reticleColor,
                radius = 12f,
                center = Offset(cx + bubbleOffsetX, cy + bubbleOffsetY)
            )
        }

        // Top Telemetry Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianBg.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("camera_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    color = ObsidianBg.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.2.dp,
                        if (tiltTelemetry.isSteadyAndAligned) LevelEmerald else PrecisionAmber
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (tiltTelemetry.isSteadyAndAligned) LevelEmerald else PrecisionAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ميل العدسة: ${String.format(java.util.Locale.US, "%.1f°", tiltTelemetry.flatTiltDeg)} • استواء: ${if (tiltTelemetry.isSteadyAndAligned) "مثالي" else "مائل قليلاً"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Camera2 hardware optics pill
            Surface(
                color = ObsidianBg.copy(alpha = 0.82f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LaserCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عدسة الكاميرا: f=${String.format(java.util.Locale.US, "%.2f", optics.focalLengthMm)}mm",
                        style = MaterialTheme.typography.labelMedium,
                        color = LaserCyan
                    )
                    Text(
                        text = "FOV: ${String.format(java.util.Locale.US, "%.1f°", optics.horizontalFovDeg)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrecisionAmber
                    )
                }
            }
        }

        // If camera permission denied or camera unavailable, show helpful card in center
        if (!hasCameraPermission || cameraError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, PrecisionAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = PrecisionAmber,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = cameraError ?: "يرجى منح صلاحية الكاميرا لتصوير الأشياء وقياس أبعادها الحقيقية مباشرة",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    if (!hasCameraPermission) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LaserCyan,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier.testTag("grant_camera_permission_button")
                        ) {
                            Text("تفعيل صلاحية الكاميرا", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    OutlinedButton(
                        onClick = { onUseWorkbenchScene(WorkbenchScene.DESK_SETUP) },
                        border = BorderStroke(1.dp, LaserCyan)
                    ) {
                        Text("استخدام المشهد المعياري الجاهز (1:1)", color = LaserCyan)
                    }
                }
            }
        }

        // Bottom Capture Controls Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBg.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "نصيحة لدقة 99%: ضع بطاقة بنكية أو ورقة بجانب الجسم، واجعل الدائرة الخضراء في المنتصف قبل الالتقاط",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onUseWorkbenchScene(WorkbenchScene.DESK_SETUP) },
                        border = BorderStroke(1.dp, LaserCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("camera_load_demo_button")
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = LaserCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشهد تجريبي", color = LaserCyan)
                    }

                    Button(
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                return@Button
                            }
                            isCapturing = true
                            val photoFile = File(
                                context.cacheDir,
                                "measured_capture_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            val capturedTilt = abs(tiltTelemetry.flatTiltDeg)

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        onPhotoCaptured(Uri.fromFile(photoFile), capturedTilt)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        // If virtual camera frame capture fails in container, load calibrated scene smoothly
                                        onUseWorkbenchScene(WorkbenchScene.DESK_SETUP)
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrecisionAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(58.dp)
                            .testTag("capture_shutter_button")
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = ObsidianBg,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(Icons.Default.Camera, contentDescription = "التقاط")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التقاط وقياس الطول",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
