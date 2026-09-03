package com.sih.netrasahayak.repository

import com.sih.netrasahayak.database.ScreeningEntity
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.network.NetraApiService
import com.sih.netrasahayak.network.dto.ScreeningUploadDto
import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Where a stored screening record goes when it is synced.
 *
 * No cloud backend has been invented for this project. This interface plus
 * [RemoteSyncDataSource] define the contract the backend team can implement;
 * [MockSyncDataSource] lets the Sync screen be demonstrated in the meantime.
 */
interface SyncDataSource {
    suspend fun upload(screening: ScreeningEntity): Outcome<Unit>
}

/**
 * Talks to POST {BASE_URL}screenings.
 *
 * That endpoint does not exist on the FastAPI server yet - until it is added
 * this returns a normal, non-crashing server error which the Sync screen shows
 * as a readable message.
 */
class RemoteSyncDataSource(private val api: NetraApiService) : SyncDataSource {

    private val tag = "RemoteSync"

    override suspend fun upload(screening: ScreeningEntity): Outcome<Unit> = try {
        val response = api.uploadScreening(
            ScreeningUploadDto(
                localId = screening.id,
                patientId = screening.patientId,
                age = screening.age,
                gender = screening.gender,
                diabetesDurationYears = screening.diabetesDurationYears,
                prediction = screening.prediction,
                confidence = screening.confidence,
                heatmapUrl = screening.heatmapUrl,
                recommendation = screening.recommendation,
                imagePath = screening.imagePath ?: "",
                heatmapPath = screening.heatmapUrl ?: "",
                createdAtEpochMillis = screening.createdAt
            )
        )
        if (response.isSuccessful) {
            Outcome.Success(Unit)
        } else {
            Outcome.Failure(AppError.ServerError(response.code()))
        }
    } catch (e: IOException) {
        Log.w(tag, "Sync upload failed", e)
        Outcome.Failure(AppError.ServerUnreachable)
    } catch (t: Throwable) {
        Log.w(tag, "Sync upload failed", t)
        Outcome.Failure(AppError.Unknown(t.message))
    }
}

/** MOCK / DEMO ONLY - accepts every record after a short pause. */
class MockSyncDataSource : SyncDataSource {
    override suspend fun upload(screening: ScreeningEntity): Outcome<Unit> {
        delay(400)
        return Outcome.Success(Unit)
    }
}
