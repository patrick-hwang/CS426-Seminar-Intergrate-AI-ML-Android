package com.example.imagetotextpathwa

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.GsonBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

enum class DemoMode {
    RAW_STRING,
    STUDENT_CARD,
    METRO_RECEIPT
}

data class CaptureConfig(
    val aspectRatio: Float = 1f,
)

private data class MenuItem(
    val mode: DemoMode,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

class MLKitDemonstration {
    private var currentMode by mutableStateOf<DemoMode?>(null)

    @Composable
    fun DemonstrationMenu() {
        when (currentMode) {
            null -> MenuContent { mode -> currentMode = mode }
            DemoMode.RAW_STRING -> RawStringScreen { currentMode = null }
            DemoMode.STUDENT_CARD -> StudentCardScreen { currentMode = null }
            DemoMode.METRO_RECEIPT -> MetroReceiptScreen { currentMode = null }
        }
    }

    @Composable
    private fun MenuContent(onModeSelected: (DemoMode) -> Unit) {
        val items = remember {
            listOf(
                MenuItem(
                    mode = DemoMode.RAW_STRING,
                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                    title = "Extract Raw Text",
                    subtitle = "Optical Character Recognition (OCR) from camera or gallery"
                ),
                MenuItem(
                    mode = DemoMode.STUDENT_CARD,
                    icon = Icons.Default.Badge,
                    title = "Student Card",
                    subtitle = "Extract info from student ID card"
                ),
                MenuItem(
                    mode = DemoMode.METRO_RECEIPT,
                    icon = Icons.Default.ConfirmationNumber,
                    title = "HCMC Metro Receipt",
                    subtitle = "Extract info from metro receipt"
                ),
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Text(
                    text = "ML Kit",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Demonstration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select a recognition mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )

                Spacer(modifier = Modifier.height(36.dp))

                items.forEach { item ->
                    val tintColor = when (item.mode) {
                        DemoMode.RAW_STRING -> MaterialTheme.colorScheme.primary
                        DemoMode.STUDENT_CARD -> MaterialTheme.colorScheme.tertiary
                        DemoMode.METRO_RECEIPT -> MaterialTheme.colorScheme.secondary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onModeSelected(item.mode) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(tintColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = tintColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun CaptureScreen(
        title: String,
        onBack: () -> Unit,
        config: CaptureConfig = CaptureConfig(),
        formatResult: (String) -> String = { it },
        sortByPosition: Boolean = false
    ) {
        val context = LocalContext.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current

        var hasCameraPermission by remember { mutableStateOf(false) }
        var recognizedText by remember {
            mutableStateOf("Place text in frame and capture or pick from gallery")
        }
        var isProcessing by remember { mutableStateOf(false) }
        var debugBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

        val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val imageCapture = remember { ImageCapture.Builder().build() }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission required!", Toast.LENGTH_SHORT).show()
            }
        }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                isProcessing = true
                recognizedText = "Reading image from gallery..."
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (originalBitmap != null) {
                        val processedBitmap = processImage(originalBitmap)
                        debugBitmap = processedBitmap
                        val inputImage = InputImage.fromBitmap(processedBitmap, 0)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val raw = if (sortByPosition) sortTextByPosition(visionText) else visionText.text
                                recognizedText = if (raw.isBlank()) "No text found!" else formatResult(raw)
                            }
                            .addOnFailureListener { e -> recognizedText = "Error: ${e.message}" }
                            .addOnCompleteListener { isProcessing = false }
                    } else {
                        recognizedText = "Cannot read this image format!"
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    recognizedText = "Error loading image: ${e.message}"
                    isProcessing = false
                }
            }
        }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        DisposableEffect(Unit) {
            onDispose {
                cameraProvider?.unbindAll()
            }
        }

        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to ML Kit Menu"
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                                val executor = ContextCompat.getMainExecutor(ctx)

                                cameraProviderFuture.addListener({
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    try {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraX", "Error", e)
                                    }
                                }, executor)
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    isProcessing = true
                                    recognizedText = "Processing image..."
                                    imageCapture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                val bitmap = imageProxy.toBitmap()
                                                val matrix = Matrix().apply {
                                                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                                }
                                                val rotated = Bitmap.createBitmap(
                                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                                )
                                                val processed = processImage(rotated)
                                                debugBitmap = processed
                                                val inputImage = InputImage.fromBitmap(processed, 0)
                                                recognizer.process(inputImage)
                                                    .addOnSuccessListener { visionText ->
                                                        val raw = if (sortByPosition) sortTextByPosition(visionText) else visionText.text
                                                        recognizedText = if (raw.isBlank()) "No text found!" else formatResult(raw)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        recognizedText = "Error: ${e.message}"
                                                    }
                                                    .addOnCompleteListener {
                                                        isProcessing = false
                                                        imageProxy.close()
                                                    }
                                            }

                                            override fun onError(e: ImageCaptureException) {
                                                isProcessing = false
                                                recognizedText = "Capture error: ${e.message}"
                                            }
                                        }
                                    )
                                },
                                enabled = !isProcessing
                            ) {
                                Text(if (isProcessing) "Processing..." else "Capture")
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            ) {
                                Text("Gallery")
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Result:",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                IconButton(
                                    onClick = {
                                        if (recognizedText.isNotBlank()) {
                                            scope.launch {
                                                val clipData = ClipData.newPlainText("OCR Text", recognizedText)
                                                clipboard.setClipEntry(clipData.toClipEntry())
                                            }
                                            Toast.makeText(context, "Result copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = recognizedText, style = MaterialTheme.typography.bodyLarge)
                        }

                        if (debugBitmap != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(120.dp)
                            ) {
                                Text(
                                    "Processed image:",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = debugBitmap!!.asImageBitmap(),
                                    contentDescription = "Processed Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.Gray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RawStringScreen(onBack: () -> Unit) {
        CaptureScreen(
            title = "Extract Raw Text",
            onBack = onBack,
            formatResult = { it }
        )
    }

    @Composable
    private fun StudentCardScreen(onBack: () -> Unit) {
        CaptureScreen(
            title = "Student Card",
            onBack = onBack,
            config = CaptureConfig(aspectRatio = 4f / 3f),
            sortByPosition = true,
            formatResult = { rawText -> extractStudentCardInfo(rawText) }
        )
    }

    @Composable
    private fun MetroReceiptScreen(onBack: () -> Unit) {
        CaptureScreen(
            title = "HCMC Metro Receipt",
            onBack = onBack,
            sortByPosition = true,
            formatResult = { rawText -> extractMetroReceiptInfo(rawText) }
        )
    }
}

private fun sortTextByPosition(text: Text): String {
    val lines = text.textBlocks.flatMap { block ->
        block.lines.map { line ->
            val box = line.boundingBox
            val lineText = line.elements.sortedBy { it.boundingBox?.left ?: 0 }
                .joinToString(" ") { it.text }
            Triple(lineText, box?.top ?: 0, box?.left ?: 0)
        }
    }
    val sorted = lines.sortedWith(compareBy({ it.second }, { it.third }))
    return sorted.joinToString("\n") { it.first }
}

private fun extractStudentCardInfo(rawText: String): String {
    val lines = rawText.lines()

    var fullName = ""
    var dob = ""
    var studentId = ""
    var faculty = ""
    var degree = ""
    var expires = ""

    for (line in lines) {
        val t = line.trim()

        if (dob.isBlank()) {
            Regex("""(\d{2}/\d{2}/\d{4})""").find(t)?.let {
                dob = it.groupValues[1]
            }
        }

        if (studentId.isBlank()) {
            Regex("""MSSV:\s*(.+?)$""").find(t)?.let {
                studentId = it.groupValues[1].filter { it.isDigit() }
            }
            if (studentId.isBlank()) {
                Regex("""ID:\s*(.+?)$""").find(t)?.let {
                    studentId = it.groupValues[1].filter { it.isDigit() }
                }
            }
        }

        if (faculty.isBlank()) {
            Regex("""Khoa\s*:?\s*(.+)$""").find(t)?.let {
                faculty = it.groupValues[1].trimStart(':').trim()
            }
            if (faculty.isBlank()) {
                Regex("""Faculty\s*(?:of)?\s*:?\s*(.+)$""").find(t)?.let {
                    faculty = it.groupValues[1].trimStart(':').trim()
                }
            }
        }

        if (degree.isBlank()) {
            Regex("""Bậc\s*:?\s*(.+)$""").find(t)?.let {
                degree = it.groupValues[1].trimStart(':').trim()
            }
            if (degree.isBlank()) {
                Regex("""Degree\s*:?\s*(.+)$""").find(t)?.let {
                    degree = it.groupValues[1].trimStart(':').trim()
                }
            }
            if (degree.isBlank()) {
                Regex("""(Đại học|Bachelor)""").find(t)?.let {
                    degree = it.groupValues[1]
                }
            }
        }

        if (expires.isBlank()) {
            Regex("""(?:H.n\s*th.|Expires)\s*:?\s*(.+)$""").find(t)?.let {
                expires = it.groupValues[1].trim()
            }
        }
    }

    for (i in lines.indices) {
        if (lines[i].contains("Ngày sinh") || lines[i].contains("Date of Birth")) {
            for (j in i - 1 downTo 0) {
                val candidate = lines[j].trim()
                if (candidate.isNotBlank() && candidate.length > 3) {
                    fullName = candidate
                    break
                }
            }
            break
        }
    }

    return buildString {
        appendLine("Full Name: ${fullName.ifBlank { "[...]" }}")
        appendLine("Date of Birth: ${dob.ifBlank { "[...]" }}")
        appendLine("Student ID: ${studentId.ifBlank { "[...]" }}")
        appendLine("Faculty: ${faculty.ifBlank { "[...]" }}")
        appendLine("Degree: ${degree.ifBlank { "[...]" }}")
        appendLine("Expires: ${expires.ifBlank { "[...]" }}")
    }
}

private fun extractMetroReceiptInfo(rawText: String): String {
    var departure = ""
    var arrival = ""
    var price = ""
    var exportTime = ""
    var boughtAt = ""

    for (line in rawText.lines()) {
        val t = line.trim()

        if (departure.isBlank() || arrival.isBlank()) {
            Regex("""(GA .+?)\s*[-–]\s*(GA .+)""").find(t)?.let {
                departure = it.groupValues[1].trim()
                arrival = it.groupValues[2].trim()
            }
        }

        if (price.isBlank()) {
            Regex("""Gia ve:\s*(.+?VND)""").find(t)?.let {
                price = it.groupValues[1].trim()
            }
        }

        if (exportTime.isBlank()) {
            Regex("""Xuat phieu luc:\s*(.+)$""").find(t)?.let {
                exportTime = it.groupValues[1].trim()
            }
        }

        if (boughtAt.isBlank()) {
            Regex("""Diem ban ve:\s*(.+)$""").find(t)?.let {
                boughtAt = it.groupValues[1].trim()
            }
        }
    }

    return buildString {
        appendLine("Departure Station: ${departure.ifBlank { "[...]" }}")
        appendLine("Arrival Station: ${arrival.ifBlank { "[...]" }}")
        appendLine("Price: ${price.ifBlank { "[...]" }}")
        appendLine("Ticket Export Timestamp: ${exportTime.ifBlank { "[...]" }}")
        appendLine("Ticket Bought At: ${boughtAt.ifBlank { "[...]" }}")
    }
}

// ==================================================================
// IMAGE PRE-PROCESSING & JSON HELPERS
// ==================================================================

fun processImage(original: Bitmap): Bitmap {
    val enhancedBitmap = Bitmap.createBitmap(
        original.width, original.height,
        original.config ?: Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(enhancedBitmap)
    val paint = Paint()

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
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
    canvas.drawBitmap(original, 0f, 0f, paint)
    return enhancedBitmap
}

private fun Rect?.toMap(): Map<String, Int>? {
    if (this == null) return null
    return mapOf(
        "left" to left,
        "top" to top,
        "right" to right,
        "bottom" to bottom,
        "width" to width(),
        "height" to height()
    )
}

fun visionTextToJson(visionText: Text): String {
    val dataMap = mapOf(
        "text" to visionText.text,
        "blocks" to visionText.textBlocks.map { block ->
            mapOf(
                "text" to block.text,
                "boundingBox" to block.boundingBox.toMap(),
                "lines" to block.lines.map { line ->
                    mapOf(
                        "text" to line.text,
                        "boundingBox" to line.boundingBox.toMap(),
                        "elements" to line.elements.map { element ->
                            mapOf(
                                "text" to element.text,
                                "boundingBox" to element.boundingBox.toMap()
                            )
                        }
                    )
                }
            )
        }
    )
    return GsonBuilder().setPrettyPrinting().create().toJson(dataMap)
}

fun saveJsonToFile(context: Context, jsonString: String, fileName: String = "ocr_output.json"): File? {
    return try {
        val fileDir = context.getExternalFilesDir(null)
        val file = File(fileDir, fileName)
        val writer = FileWriter(file)
        writer.write(jsonString)
        writer.flush()
        writer.close()
        Log.d("OCR_FILE_SAVE", "JSON file saved at: ${file.absolutePath}")
        file
    } catch (e: IOException) {
        Log.e("OCR_FILE_SAVE", "Error writing JSON file: ${e.message}", e)
        null
    }
}
