package com.sih.netrasahayak.model

/**
 * A completed AI screening, as shown on the result screen.
 *
 * This is the app's own domain model. It is produced from the backend's
 * PredictionResponse, from the mock repository, or - later - from an on-device
 * TensorFlow Lite model. The UI never sees a network DTO directly.
 */
data class ScreeningResult(
    val drClass: DrClass,
    /** Exactly what the backend said, kept for the record. */
    val rawPrediction: String,
    /** 0.0 .. 1.0 */
    val confidence: Float,
    /**
     * Fully-resolved location of the Grad-CAM heatmap, or null when the backend
     * did not send one. May be an http(s) URL or a local file path (mock mode).
     */
    val heatmapUrl: String?,
    val recommendation: String
) {
    val confidencePercent: Int get() = (confidence.coerceIn(0f, 1f) * 100).toInt()
    val hasHeatmap: Boolean get() = !heatmapUrl.isNullOrBlank()
}
