package com.example.imagetotextpathwa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TFLiteDemonstration(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val classifier = remember { TransactionClassifier(context) }

    // FIX: Sử dụng String thay vì TextFieldValue để tránh lỗi desync con trỏ với Unikey trên Emulator
    var inputText by rememberSaveable {
        mutableStateOf("")
    }
    var result by remember { mutableStateOf<ClassificationResult?>(null) }

    val examples = listOf(
        "Ăn sáng phở bò",
        "Mua xăng đi làm",
        "Đóng tiền điện tháng 7",
        "Học phí học kỳ 2",
        "Mua đồ trên Shopee",
        "Lương tháng này",
        "Đi xem phim rạp",
        "Mua thuốc đau đầu"
    )

    fun performClassification() {
        if (inputText.isNotBlank()) {
            result = classifier.classify(inputText)
            keyboardController?.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
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
                            contentDescription = "Return to Home"
                        )
                    }
                    Text(
                        text = "TensorFlow Lite",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Phân loại chi tiêu",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Nhập nội dung chi tiêu để máy phân loại tự động",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nội dung chi tiêu") },
                placeholder = { Text("Ví dụ: Ăn bún chả") },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = ""; result = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { performClassification() }
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { performClassification() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = inputText.isNotBlank()
            ) {
                Text("Phân loại ngay", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = result != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                result?.let { res ->
                    ResultCard(res)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ví dụ gợi ý:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                examples.forEach { example ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            inputText = example
                            result = classifier.classify(example)
                        },
                        label = { Text(example) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ResultCard(result: ClassificationResult) {
    val (icon, color) = getCategoryInfo(result.label)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "Kết quả dự đoán:",
                    style = MaterialTheme.typography.labelMedium,
                    color = color.copy(alpha = 0.8f)
                )
                Text(
                    text = result.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Độ tin cậy: ${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun getCategoryInfo(label: String): Pair<ImageVector, Color> {
    return when (label) {
        "Di chuyển" -> Icons.Default.DirectionsCar to Color(0xFF2196F3)
        "Giáo dục" -> Icons.Default.School to Color(0xFF9C27B0)
        "Giải trí" -> Icons.Default.Celebration to Color(0xFFFF9800)
        "Hóa đơn" -> Icons.AutoMirrored.Filled.ReceiptLong to Color(0xFFF44336)
        "Khác" -> Icons.Default.Category to Color(0xFF607D8B)
        "Mua sắm" -> Icons.Default.ShoppingCart to Color(0xFFE91E63)
        "Sức khỏe" -> Icons.Default.MedicalServices to Color(0xFF4CAF50)
        "Thu nhập" -> Icons.Default.Payments to Color(0xFF4CAF50)
        "Tài chính" -> Icons.Default.AccountBalance to Color(0xFF3F51B5)
        "Ăn uống" -> Icons.Default.Restaurant to Color(0xFF795548)
        else -> Icons.Default.Category to Color.Gray
    }
}