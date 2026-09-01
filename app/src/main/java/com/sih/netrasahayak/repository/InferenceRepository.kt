package com.sih.netrasahayak.repository

import android.net.Uri
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult

/**
 * The one place the app asks "what does the AI say about this image?".
 *
 * Implementations:
 *   - [RemoteInferenceRepository] : POST /predict to the FastAPI + EfficientNet
 *                                   backend (online, includes Grad-CAM).
 *   - [MockInferenceRepository]   : development only, no server needed.
 *   - [LocalInferenceRepository]  : reserved for on-device TensorFlow Lite
 *                                   inference (offline). Not implemented yet.
 *
 * The ViewModel depends on this interface only, so switching implementations is
 * a one-line change in [com.sih.netrasahayak.di.ServiceLocator].
 */
interface InferenceRepository {

    /** Shown on the loading screen, e.g. "Screening server" or "Demo mode". */
    val sourceName: String

    /** True when this implementation needs a working internet connection. */
    val requiresInternet: Boolean

    suspend fun analyze(imageUri: Uri, patient: PatientDetails): Outcome<ScreeningResult>
}
