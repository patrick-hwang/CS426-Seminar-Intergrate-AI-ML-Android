# Integrating AI and Machine Learning in Android Applications

## 1. Overview

This project presents a reference implementation for integrating two prominent Google AI/ML frameworks within a native Android application built with Jetpack Compose. The application demonstrates on-device intelligence through two distinct pipelines: **optical character recognition (OCR)** via ML Kit, and **text classification** via a custom TensorFlow Lite model.

## 2. System Requirements

| Component | Requirement |
|-----------|------------|
| Android OS | Android 16.0 (API Level 36) or higher |
| Build Tool | Android Gradle Plugin 9.2.1 |
| Gradle | 9.5.0 (wrapped) |
| IDE | Android Studio (latest stable release recommended) |
| Hardware (optional) | Camera for live OCR capture |

### 2.1. Emulator Configuration

If using an Android Virtual Device (AVD), create a device with API Level 36.0 (Android 16.0). The recommended emulated device is **Pixel 10 Pro XL**.

### 2.2. Physical Device

A physical device running Android 16.0 (API Level 36) or later is also supported.

## 3. Setup Instructions

### 3.1. Opening the Project

1. Launch Android Studio.
2. Select **File > Open** and navigate to the project root directory.
3. Wait for Android Studio to initialize the project and index the source files.

### 3.2. Gradle Synchronization

1. After opening the project, Android Studio will prompt a Gradle sync. Click **Sync Now**.
2. Alternatively, trigger a manual sync via **File > Sync Project with Gradle Files**.
3. Verify that the sync completes without errors. The version catalog (`gradle/libs.versions.toml`) manages all dependency versions centrally.

### 3.3. Clean Build

Execute a clean build to verify the environment:

```bash
# Windows (PowerShell)
./gradlew clean assembleDebug

# macOS / Linux
./gradlew clean assembleDebug
```

A successful build confirms that all dependencies are resolved and the project compiles correctly.

## 4. Running the Application

### 4.1. Deployment

1. Connect a target device (physical or emulator) via ADB.
2. Select the target device from the Android Studio device dropdown.
3. Click **Run** (Shift + F10) or execute:

```bash
./gradlew installDebug
```

### 4.2. Application Features

Upon launch, the application presents a home screen with two primary demonstration modules:

#### Module A: ML Kit Demo — Optical Character Recognition

Uses Google's ML Kit Text Recognition API (`com.google.mlkit:text-recognition`) to extract text from images captured via CameraX or selected from the gallery. The demo includes three operational modes:

| Mode | Functionality |
|------|---------------|
| **Raw String** | Generic OCR: extracts all visible text from a captured image and displays the raw output. |
| **Student Card** | Structured extraction: parses Vietnamese university student ID cards and returns fields including full name, date of birth, student ID (MSSV), faculty (Khoa), degree level (Bậc), and card expiry (Hạn thẻ). The parser applies regex-based post-processing to handle common OCR artifacts and variations in label placement. |
| **Metro Receipt** | Structured extraction: parses HCMC Metro receipts and returns departure station, arrival station, fare price, and timestamps. |

Additional features include:
- Grayscale + contrast/brightness image preprocessing prior to OCR.
- Spatial text sorting based on bounding-box coordinates.
- JSON serialization of results via Gson.
- One-click copy to clipboard and save to file.

#### Module B: TensorFlow Lite Demo — Text Classification

Demonstrates on-device inference using a custom TensorFlow Lite model. The classifier categorizes Vietnamese transaction descriptions into the following financial categories:

| Category | Label | Example |
|----------|-------|---------|
| Ăn uống | Food | "Ăn sáng phở bò" |
| Di chuyển | Transport | "Mua xăng đi làm" |
| Giáo dục | Education | "Đóng học phí" |
| Giải trí | Entertainment | "Xem phim" |
| Hóa đơn | Bills | "Đóng tiền điện" |
| Mua sắm | Shopping | "Mua quần áo" |
| Sức khỏe | Health | "Khám bệnh" |
| Thu nhập | Income | "Nhận lương" |
| Tài chính | Finance | "Chuyển khoản" |
| Khác | Other | — |

The inference pipeline:
1. Loads the TFLite model (`transaction_classifier.tflite`) and vocabulary (`vocab.json`) from assets.
2. Tokenizes input text using NFC normalization and Unicode-aware punctuation removal.
3. Runs inference via `TensorFlow Lite Interpreter`.
4. Returns the predicted category with a confidence score.

## 5. Project Structure

```
app/
├── src/main/
│   ├── java/com/example/imagetotextpathwa/
│   │   ├── MainActivity.kt              # Entry point & navigation
│   │   ├── MainHomeScreen.kt            # Home screen with module selection
│   │   ├── MLKitDemonstration.kt        # OCR pipelines (raw, student card, metro)
│   │   ├── TFLiteDemonstration.kt       # Text classification UI
│   │   ├── TransactionClassifier.kt     # TFLite inference engine
│   │   └── ui/theme/                    # Material3 theme (Color, Type, Theme)
│   ├── assets/
│   │   ├── transaction_classifier.tflite # Custom TFLite model
│   │   ├── vocab.json                    # Word-to-integer vocabulary
│   │   └── labels.txt                    # Classification labels
│   └── res/                              # Resources (layouts, strings, icons)
├── gradle/
│   ├── libs.versions.toml                # Version catalog
│   └── wrapper/                          # Gradle wrapper (9.5.0)
├── build.gradle.kts                      # Root build configuration
└── settings.gradle.kts                   # Project settings
```

## 6. Dependencies

| Library | Purpose |
|---------|---------|
| `com.google.mlkit:text-recognition` | OCR text extraction |
| `androidx.camera:camera-*` | CameraX for live preview and capture |
| `org.tensorflow:tensorflow-lite` | On-device ML inference |
| `org.tensorflow:tensorflow-lite-support` | TFLite support utilities |
| `com.google.code.gson:gson` | JSON serialization |
| `androidx.compose.material3` | Material 3 UI components |
| `androidx.compose.material:material-icons-extended` | Extended icon set |

## 7. Notes

- Camera permission is requested at runtime. The feature is optional — the application functions with gallery images on devices without a camera.
- The TFLite model file is excluded from APK compression (`noCompress "tflite"` in the build configuration).
- The student card parser is tuned for the layout conventions of Vietnamese university ID cards issued by member universities of Vietnam National University, Ho Chi Minh City.
