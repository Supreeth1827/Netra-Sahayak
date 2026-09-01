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
    data class Error(val error: AppError) : AnalysisState
}

data class ScreeningUiState(
    val patient: PatientDetails? = null,
    val imageUri: Uri? = null,
    val analysis: AnalysisState = AnalysisState.Idle,
    /** Row id of the record written to Room, once the screening is saved. */
    val savedRecordId: Long? = null
)

/**
 * Drives the whole PATIENT -> IMAGE -> ANALYSE -> RESULT flow.
 *
 * One instance is shared by every screen in the "screening" navigation graph, so
 * the patient details and the chosen image survive navigation without being
 * squeezed into route arguments.
 *
 * The UI never touches Retrofit or Room - it only calls these methods.
 */
class ScreeningViewModel(
    private val inferenceRepository: InferenceRepository,
    private val screeningRepository: ScreeningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreeningUiState())
    val uiState: StateFlow<ScreeningUiState> = _uiState.asStateFlow()

    /** Name of the engine doing the work, e.g. "Demo mode (no server)". */
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

    /** Called after a capture or a gallery pick. Nothing is uploaded here. */
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

    /**
     * Uploads the image and asks for a screening result. Only ever triggered by
     * the ANALYZE IMAGE button.
     *
     * A second tap while a request is in flight is ignored, so an image can
     * never be submitted twice.
     */
    fun analyze() {
        val state = _uiState.value

        if (state.analysis is AnalysisState.Analyzing) return // duplicate submission guard

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
                    // Save locally straight away: the record must survive even if
                    // the phone goes offline right after the result arrives.
                    val saved = screeningRepository.save(patient, imageUri, outcome.data)
                    _uiState.update {
                        it.copy(
                            analysis = AnalysisState.Success(outcome.data),
                            savedRecordId = (saved as? Outcome.Success)?.data
                        )
                    }
                }

                is Outcome.Failure -> {
                    _uiState.update { it.copy(analysis = AnalysisState.Error(outcome.error)) }
                }
            }
        }
    }

    /** Clears everything so the next patient starts from a blank form. */
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
