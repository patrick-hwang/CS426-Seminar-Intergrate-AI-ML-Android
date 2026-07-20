package com.example.imagetotextpathwa

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdvancedOCRApp()
                }
            }
        }
    }
}

@Composable
fun AdvancedOCRApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("Đưa chữ vào khung và bấm chụp") }
    var isProcessing by remember { mutableStateOf(false) }
    var debugBitmap by remember { mutableStateOf<Bitmap?>(null) } // Hiển thị ảnh đã xử lý

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) Toast.makeText(context, "Cần quyền Camera!", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // 1. Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraX", "Lỗi", e)
                            }
                        }, executor)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Vẽ khung ngắm (Bounding Box) làm mờ xung quanh
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val boxWidth = canvasWidth * 0.75f
                    val boxHeight = canvasHeight * 0.6f // Khung hình chữ nhật dẹt

                    // Vẽ nền đen mờ
                    drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)

                    // Đục lỗ trong suốt ở giữa
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset((canvasWidth - boxWidth) / 2, (canvasHeight - boxHeight) / 2),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                }

                // Vẽ viền khung ngắm
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .fillMaxHeight(0.6f)
                        .align(Alignment.Center)
                        .border(2.dp, Color.Green, RoundedCornerShape(16.dp))
                )

                // 3. Nút chụp ảnh
                Button(
                    onClick = {
                        isProcessing = true
                        recognizedText = "Đang xử lý ảnh gốc..."

                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    // Chuyển ảnh thành Bitmap và xoay đúng chiều
                                    val bitmap = imageProxy.toBitmap()
                                    val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                    // TIỀN XỬ LÝ: Cắt khung và lọc trắng đen, tăng tương phản
                                    val processedBitmap = processImage(rotatedBitmap)
                                    debugBitmap = processedBitmap // Gán để hiển thị lên màn hình kiểm tra

                                    // Đưa ảnh đã qua xử lý vào ML Kit
                                    val inputImage = InputImage.fromBitmap(processedBitmap, 0)
                                    recognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            recognizedText = if (visionText.text.isBlank()) "Không tìm thấy chữ!" else visionText.text
                                        }
                                        .addOnFailureListener { e -> recognizedText = "Lỗi: ${e.message}" }
                                        .addOnCompleteListener {
                                            isProcessing = false
                                            imageProxy.close()
                                        }
                                }
                                override fun onError(e: ImageCaptureException) {
                                    isProcessing = false
                                    recognizedText = "Lỗi chụp: ${e.message}"
                                }
                            }
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "Đang xử lý..." else "📸 Quét Vùng Chọn")
                }
            }
        }

        // Khu vực hiển thị kết quả
        Surface(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Cột hiển thị chữ
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(text = "Kết quả OCR:", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                    Text(text = recognizedText, style = MaterialTheme.typography.bodyLarge)
                }

                // Cột hiển thị ảnh nhỏ đã qua xử lý (để bạn xem hiệu ứng)
                if (debugBitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
                        Text("Ảnh đã xử lý:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            bitmap = debugBitmap!!.asImageBitmap(),
                            contentDescription = "Processed Image",
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CÁC HÀM TIỀN XỬ LÝ ẢNH (PRE-PROCESSING)
// ==========================================
fun processImage(original: Bitmap): Bitmap {
    // 1. Cắt ảnh (Cropping) - Lấy đúng tỷ lệ 80% Rộng x 25% Cao ở chính giữa
    val cropWidth = (original.width * 0.75f).toInt()
    val cropHeight = (original.height * 0.6f).toInt()
    val cropX = (original.width - cropWidth) / 2
    val cropY = (original.height - cropHeight) / 2

    val croppedBitmap = Bitmap.createBitmap(original, cropX, cropY, cropWidth, cropHeight)

    // 2. Chuyển Trắng Đen & Tăng Tương Phản (Binarization)
    val enhancedBitmap = Bitmap.createBitmap(croppedBitmap.width, croppedBitmap.height, croppedBitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(enhancedBitmap)
    val paint = Paint()

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f) // Biến thành Trắng Đen (Grayscale)

    // Tăng độ tương phản (Contrast = 1.5) và giảm độ sáng xíu (Brightness = -20)
    val contrast = 1.5f
    val brightness = -20f
    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, brightness,
        0f, contrast, 0f, 0f, brightness,
        0f, 0f, contrast, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))
    colorMatrix.postConcat(contrastMatrix)

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(croppedBitmap, 0f, 0f, paint)

    return enhancedBitmap
}