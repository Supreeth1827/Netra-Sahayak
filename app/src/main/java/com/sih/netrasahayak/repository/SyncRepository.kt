package com.sih.netrasahayak.repository

import com.sih.netrasahayak.database.ScreeningDao
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.network.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Outcome of one "SYNC NOW" run. */
data class SyncSummary(
    val uploaded: Int,
    val failed: Int,
    val error: AppError? = null
)

/**
 * Pushes locally stored screenings to a server when connectivity allows.
 *
 * Offline-first: records are always written to Room first and marked synced only
 * after the server confirms. Nothing in the app waits on this.
 */
class SyncRepository(
    private val dao: ScreeningDao,
    private val connectivity: ConnectivityObserver,
    private val dataSource: SyncDataSource
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
            when (val result = dataSource.upload(record)) {
                is Outcome.Success -> {
                    runCatching { dao.markSynced(record.id) }
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
}
