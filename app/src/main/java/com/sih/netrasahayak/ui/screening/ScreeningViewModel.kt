package com.sih.netrasahayak.ui.screening

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sih.netrasahayak.di.ServiceLocator
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Gender
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult
import com.sih.netrasahayak.repository.InferenceRepository
import com.sih.netrasahayak.repository.ScreeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the analysis request currently stands. */
sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Analyzing : AnalysisState
    data class Success(val result: ScreeningResult) : AnalysisState
    data class QueuedOffline(val recordId: Long) : AnalysisState
    data class Error(val error: AppError) : AnalysisState
}

data class ScreeningUiState(
    val patient: PatientDetails? = null,
    val imageUri: Uri? = null,
    val analysis: AnalysisState = AnalysisState.Idle,
    /** Row id of the record written to Room, once the screening is saved. */
    val savedRecordId: Long? = null
)

class ScreeningViewModel(
    private val inferenceRepository: InferenceRepository,
    private val screeningRepository: ScreeningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreeningUiState())
    val uiState: StateFlow<ScreeningUiState> = _uiState.asStateFlow()

    val inferenceSourceName: String get() = inferenceRepository.sourceName

    fun setPatient(
        patientId: String,
        age: Int,
        gender: Gender?,
        diabetesDurationYears: Int?
    ) {
        _uiState.update {
            it.copy(
                patient = PatientDetails(
                    patientId = patientId.trim(),
                    age = age,
                    gender = gender,
                    diabetesDurationYears = diabetesDurationYears
                )
            )
        }
    }

    fun setImage(uri: Uri) {
        _uiState.update {
            it.copy(imageUri = uri, analysis = AnalysisState.Idle, savedRecordId = null)
        }
    }

    fun clearImage() {
        _uiState.update {
            it.copy(imageUri = null, analysis = AnalysisState.Idle, savedRecordId = null)
        }
    }

    fun dismissError() {
        if (_uiState.value.analysis is AnalysisState.Error) {
            _uiState.update { it.copy(analysis = AnalysisState.Idle) }
        }
    }

    fun analyze() {
        val state = _uiState.value

        if (state.analysis is AnalysisState.Analyzing) return

        val patient = state.patient
        val imageUri = state.imageUri

        if (imageUri == null) {
            _uiState.update { it.copy(analysis = AnalysisState.Error(AppError.NoImageSelected)) }
            return
        }
        if (patient == null) {
            _uiState.update { it.copy(analysis = AnalysisState.Error(AppError.Unknown())) }
            return
        }

        _uiState.update { it.copy(analysis = AnalysisState.Analyzing) }

        viewModelScope.launch {
            when (val outcome = inferenceRepository.analyze(imageUri, patient)) {
                is Outcome.Success -> {
                    val saved = screeningRepository.save(patient, imageUri, outcome.data)
                    _uiState.update {
                        it.copy(
                            analysis = AnalysisState.Success(outcome.data),
                            savedRecordId = (saved as? Outcome.Success)?.data
                        )
                    }
                }

                is Outcome.Failure -> {
                    if (outcome.error == AppError.NoInternet) {
                        when (val queued = screeningRepository.savePending(patient, imageUri)) {
                            is Outcome.Success -> {
                                _uiState.update {
                                    it.copy(
                                        analysis = AnalysisState.QueuedOffline(queued.data),
                                        savedRecordId = queued.data
                                    )
                                }
                            }
                            is Outcome.Failure -> {
                                _uiState.update {
                                    it.copy(analysis = AnalysisState.Error(queued.error))
                                }
                            }
                        }
                    } else {
                        _uiState.update { it.copy(analysis = AnalysisState.Error(outcome.error)) }
                    }
                }
            }
        }
    }

    fun reset() {
        _uiState.value = ScreeningUiState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScreeningViewModel(
                    inferenceRepository = ServiceLocator.inferenceRepository,
                    screeningRepository = ServiceLocator.screeningRepository
                )
            }
        }
    }
}
