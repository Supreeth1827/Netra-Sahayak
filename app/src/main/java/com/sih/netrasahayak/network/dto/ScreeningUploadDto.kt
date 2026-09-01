package com.sih.netrasahayak.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Shape of a stored screening record when it is pushed to a server during sync.
 *
 * NOTE: no sync endpoint has been built yet. This DTO and the matching call in
 * [com.sih.netrasahayak.network.NetraApiService] define the contract so the
 * backend team can implement it; until then sync runs against the mock source.
 */
data class ScreeningUploadDto(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("age") val age: Int,
    @SerializedName("gender") val gender: String?,
    @SerializedName("diabetes_duration") val diabetesDurationYears: Int?,
    @SerializedName("prediction") val prediction: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("heatmap_url") val heatmapUrl: String?,
    @SerializedName("recommendation") val recommendation: String,
    @SerializedName("created_at") val createdAtEpochMillis: Long
)
