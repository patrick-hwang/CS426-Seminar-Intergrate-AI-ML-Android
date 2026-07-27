# Implementation Plan - Fix TFLite Classification Inconsistency

This plan aims to resolve the discrepancy between the Python model output and the Android TFLite inference result.

## User Review Required

> [!IMPORTANT]
> I will be investigating the model's internal requirements (input type, padding). If you have the Python preprocessing code (how you tokenize and pad the input), please share it to speed up the fix.

## Proposed Changes

### [Component] Machine Learning Logic

#### [MODIFY] [TransactionClassifier.kt](file:///E:/APCS/6_CS426_Mobile_Device_Application_Development/CS426-Seminar-Intergrate-AI-ML-Android/app/src/main/java/com/example/imagetotextpathwa/TransactionClassifier.kt)
- **Log Tensor Details**: Add logging to inspect `dataType` and `shape` of input/output tensors during initialization.
- **Check Padding Strategy**: Test if the model requires **Pre-padding** (common in Keras/TensorFlow NLP models) instead of the current Post-padding.
- **Verify Input Type**: Ensure the input is converted to `FloatArray` if the model's input tensor expects `FLOAT32`.
- **Improve Tokenization**: Check if basic abbreviations like "cf", "ck" need mapping or if the current regex is too aggressive.

### [Component] UI / Debugging

#### [MODIFY] [TFLiteDemonstration.kt](file:///E:/APCS/6_CS426_Mobile_Device_Application_Development/CS426-Seminar-Intergrate-AI-ML-Android/app/src/main/java/com/example/imagetotextpathwa/TFLiteDemonstration.kt)
- Add temporary debug logs to show the generated token IDs before inference.

## Verification Plan

### Automated Tests
- Build and run the app.
- Check Logcat for "TFLite_Debug" tags to verify token IDs and tensor types.

### Manual Verification
- Test input: "ck hland cf".
- Compare the predicted index with Python's output.
- Verify that "Khác" (index 4) can be reached.
