package com.sih.netrasahayak.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Exactly what FastAPI's POST /predict is expected to return:
 *
 * {
 *   "prediction": "Moderate Diabetic Retinopathy",
 *   "confidence": 0.91,
 *   "heatmap_url": "/results/abc123_heatmap.jpg",
 *   "recommendation": "Clinical evaluation by an ophthalmologist is recommended."
 * }
 *
 * Every field is nullable so a malformed reply produces a clean error message
 * instead of a crash.
 */
data class PredictionResponseDto(
    @SerializedName("prediction") val prediction: String? = null,
    @SerializedName("confidence") val confidence: Float? = null,
    @SerializedName("heatmap_url") val heatmapUrl: String? = null,
    @SerializedName("recommendation") val recommendation: String? = null
)
