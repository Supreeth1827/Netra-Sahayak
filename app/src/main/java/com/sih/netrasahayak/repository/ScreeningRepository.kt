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

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        runCatching { dao.deleteById(id) }
        Unit
    }
}
