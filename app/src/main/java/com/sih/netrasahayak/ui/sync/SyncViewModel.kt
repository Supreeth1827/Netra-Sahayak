package com.sih.netrasahayak.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sih.netrasahayak.di.ServiceLocator
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.repository.SyncRepository
import com.sih.netrasahayak.repository.SyncSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Done(val summary: SyncSummary) : SyncState
    data class Failed(val error: AppError) : SyncState
}

/**
 * Backs the Sync screen: connection status, how many screenings are still
 * waiting, and the SYNC NOW action.
 */
class SyncViewModel(private val repository: SyncRepository) : ViewModel() {

    val isOnline: StateFlow<Boolean> = repository.observeConnectivity()
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.isOnline()
        )

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun syncNow() {
        if (_syncState.value is SyncState.Syncing) return // no double runs

        _syncState.value = SyncState.Syncing
        viewModelScope.launch {
            _syncState.value = when (val outcome = repository.syncNow()) {
                is Outcome.Success -> SyncState.Done(outcome.data)
                is Outcome.Failure -> SyncState.Failed(outcome.error)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SyncViewModel(ServiceLocator.syncRepository) }
        }
    }
}
