package com.sih.netrasahayak.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sih.netrasahayak.camera.ImageUtils
import com.sih.netrasahayak.database.ScreeningDao
import com.sih.netrasahayak.database.ScreeningEntity
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Local screening history. Everything here works with no internet.
 */
class ScreeningRepository(
    private val context: Context,
    private val dao: ScreeningDao
) {

    private val tag = "ScreeningRepository"

    fun observeHistory(): Flow<List<ScreeningEntity>> = dao.observeAll()

    fun observePendingSyncCount(): Flow<Int> = dao.observePendingCount()

    suspend fun getById(id: Long): ScreeningEntity? = withContext(Dispatchers.IO) {
        runCatching { dao.getById(id) }.getOrNull()
    }

    /**
     * Saves a finished screening. The retinal image is copied into app-private
     * storage first so it is still viewable in history later.
     */
    suspend fun save(
        patient: PatientDetails,
        imageUri: Uri,
        result: ScreeningResult
    ): Outcome<Long> = withContext(Dispatchers.IO) {
        try {
            val storedImage = ImageUtils.copyOriginalToAppStorage(context, imageUri)
            val id = dao.insert(
                ScreeningEntity(
                    patientId = patient.patientId.trim(),
                    age = patient.age,
                    gender = patient.gender?.apiValue,
                    diabetesDurationYears = patient.diabetesDurationYears,
                    imagePath = storedImage?.absolutePath,
                    heatmapUrl = result.heatmapUrl,
                    prediction = result.drClass.apiLabel,
                    confidence = result.confidence,
                    recommendation = result.recommendation,
                    synced = false
                )
            )
            Outcome.Success(id)
        } catch (t: Throwable) {
            Log.w(tag, "Could not save screening", t)
            Outcome.Failure(AppError.StorageError)
        }
    }

    /**
     * Offline-first capture: copy the original image into private storage and
     * create a pending row immediately. No network or AI inference is needed.
     */
    suspend fun savePending(
        patient: PatientDetails,
        imageUri: Uri
    ): Outcome<Long> = withContext(Dispatchers.IO) {
        try {
            val storedImage = ImageUtils.copyOriginalToAppStorage(context, imageUri)
                ?: return@withContext Outcome.Failure(AppError.StorageError)

            val id = dao.insert(
                ScreeningEntity(
                    patientId = patient.patientId.trim(),
                    age = patient.age,
                    gender = patient.gender?.apiValue,
                    diabetesDurationYears = patient.diabetesDurationYears,
                    imagePath = storedImage.absolutePath,
                    heatmapUrl = null,
                    prediction = ScreeningEntity.PENDING_PREDICTION,
                    confidence = 0f,
                    recommendation = ScreeningEntity.PENDING_RECOMMENDATION,
                    synced = false
                )
            )
            Outcome.Success(id)
        } catch (t: Throwable) {
            Log.w(tag, "Could not save offline screening", t)
            Outcome.Failure(AppError.StorageError)
        }
    }

    /**
     * Replaces the pending placeholder with the real server result.
     */
    suspend fun completePending(
        id: Long,
        result: ScreeningResult
    ) = withContext(Dispatchers.IO) {
        dao.updateAnalysis(
            id = id,
            prediction = result.drClass.apiLabel,
            confidence = result.confidence,
            heatmapUrl = result.heatmapUrl,
            recommendation = result.recommendation,
            synced = true
        )
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        runCatching { dao.deleteById(id) }
        Unit
    }
}
