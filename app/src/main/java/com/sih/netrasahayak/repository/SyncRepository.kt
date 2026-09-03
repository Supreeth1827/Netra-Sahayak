package com.sih.netrasahayak.repository

import android.net.Uri
import com.sih.netrasahayak.database.ScreeningDao
import com.sih.netrasahayak.database.ScreeningEntity
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Gender
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.network.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class SyncSummary(
    val uploaded: Int,
    val failed: Int,
    val error: AppError? = null
)

/**
 * Pushes locally stored screenings to the server when connectivity allows.
 *
 * A pending offline record is analysed through the normal /predict endpoint.
 * That endpoint both performs EfficientNet + Grad-CAM and stores the completed
 * screening on the FastAPI side. The local Room row is then updated and marked
 * synced.
 */
class SyncRepository(
    private val dao: ScreeningDao,
    private val connectivity: ConnectivityObserver,
    private val dataSource: SyncDataSource,
    private val inferenceRepository: InferenceRepository
) {

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    fun observeConnectivity(): Flow<Boolean> = connectivity.observe()

    fun isOnline(): Boolean = connectivity.isOnline()

    suspend fun syncNow(): Outcome<SyncSummary> = withContext(Dispatchers.IO) {
        if (!connectivity.isOnline()) return@withContext Outcome.Failure(AppError.NoInternet)

        val pending = runCatching { dao.getUnsynced() }.getOrElse {
            return@withContext Outcome.Failure(AppError.StorageError)
        }

        if (pending.isEmpty()) return@withContext Outcome.Success(SyncSummary(0, 0))

        var uploaded = 0
        var failed = 0
        var firstError: AppError? = null

        for (record in pending) {
            val result = if (record.isPendingAnalysis) {
                syncPendingAnalysis(record)
            } else {
                when (val uploadResult = dataSource.upload(screening = record)) {
                    is Outcome.Success -> {
                        dao.markSynced(record.id)
                        uploadResult
                    }

                    is Outcome.Failure -> uploadResult
                }
            }

            when (result) {
                is Outcome.Success -> {
                    uploaded++
                }
                is Outcome.Failure -> {
                    failed++
                    if (firstError == null) firstError = result.error
                }
            }
        }

        Outcome.Success(SyncSummary(uploaded, failed, firstError))
    }

    private suspend fun syncPendingAnalysis(
        record: ScreeningEntity
    ): Outcome<Unit> {
        val path = record.imagePath ?: return Outcome.Failure(AppError.StorageError)
        val file = File(path)
        if (!file.exists()) return Outcome.Failure(AppError.StorageError)

        val patient = PatientDetails(
            patientId = record.patientId,
            age = record.age,
            gender = Gender.fromApiValue(record.gender),
            diabetesDurationYears = record.diabetesDurationYears
        )

        return when (
            val outcome = inferenceRepository.analyze(
                imageUri = Uri.fromFile(file),
                patient = patient
            )
        ) {
            is Outcome.Success -> {
                runCatching {
                    dao.updateAnalysis(
                        id = record.id,
                        prediction = outcome.data.drClass.apiLabel,
                        confidence = outcome.data.confidence,
                        heatmapUrl = outcome.data.heatmapUrl,
                        recommendation = outcome.data.recommendation,
                        synced = true
                    )
                }.fold(
                    onSuccess = { Outcome.Success(Unit) },
                    onFailure = { Outcome.Failure(AppError.StorageError) }
                )
            }
            is Outcome.Failure -> Outcome.Failure(outcome.error)
        }
    }
}
