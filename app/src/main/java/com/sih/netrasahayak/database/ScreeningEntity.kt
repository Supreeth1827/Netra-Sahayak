package com.sih.netrasahayak.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One screening, stored on the phone. This is the offline source of truth:
 * history is fully readable with no internet connection.
 */
@Entity(tableName = "screenings")
data class ScreeningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "age") val age: Int,
    @ColumnInfo(name = "gender") val gender: String? = null,
    @ColumnInfo(name = "diabetes_duration_years") val diabetesDurationYears: Int? = null,

    /** Absolute path of the retinal image copied into the app's private storage. */
    @ColumnInfo(name = "image_path") val imagePath: String? = null,

    /** Grad-CAM heatmap location (http URL from the backend, or a local file). */
    @ColumnInfo(name = "heatmap_url") val heatmapUrl: String? = null,

    @ColumnInfo(name = "prediction") val prediction: String,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "recommendation") val recommendation: String,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),

    /** false until the record has been pushed to a server. Drives the Sync screen. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
) {
    companion object {
        /** Sentinel used for a screening captured offline but not analysed yet. */
        const val PENDING_PREDICTION = "Pending offline analysis"

        const val PENDING_RECOMMENDATION =
            "Screening captured offline. Connect to the server and use Sync Data to analyse this image."
    }

    val isPendingAnalysis: Boolean
        get() = prediction == PENDING_PREDICTION
}
