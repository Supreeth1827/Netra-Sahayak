package com.sih.netrasahayak.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sih.netrasahayak.database.ScreeningEntity
import com.sih.netrasahayak.di.ServiceLocator
import com.sih.netrasahayak.repository.ScreeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Reads the local screening history. Works with no internet. */
class HistoryViewModel(
    private val repository: ScreeningRepository
) : ViewModel() {

    val screenings: StateFlow<List<ScreeningEntity>> = repository.observeHistory()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(ServiceLocator.screeningRepository) }
        }
    }
}

/** Loads one saved screening for the detail screen. */
class HistoryDetailViewModel(
    private val repository: ScreeningRepository
) : ViewModel() {

    private val _screening = MutableStateFlow<ScreeningEntity?>(null)
    val screening: StateFlow<ScreeningEntity?> = _screening.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _loading.value = true
            _screening.value = repository.getById(id)
            _loading.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryDetailViewModel(ServiceLocator.screeningRepository) }
        }
    }
}
