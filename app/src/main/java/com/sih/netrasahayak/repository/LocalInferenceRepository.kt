package com.sih.netrasahayak.repository

import android.net.Uri
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult

/**
 * FUTURE: fully offline screening with a TensorFlow Lite EfficientNet bundled in
 * the APK.
 *
 * The integration point is ready; the model itself is trained separately in
 * Python and is not part of this version. To enable it later:
 *
 *   1. Add  implementation("org.tensorflow:tensorflow-lite:<version>")
 *      and   implementation("org.tensorflow:tensorflow-lite-support:<version>")
 *   2. Drop  app/src/main/assets/dr_efficientnet.tflite  into the project.
 *   3. Implement [analyze] below: decode -> resize to the model's input size ->
 *      normalise -> run the interpreter -> softmax -> map the argmax index onto
 *      DrClass via DrClass.fromApi(index.toString()).
 *   4. Point ServiceLocator.inferenceRepository at this class when the device is
 *      offline.
 *
 * Until then it fails with a clear message instead of pretending to work.
 */
class LocalInferenceRepository : InferenceRepository {

    override val sourceName: String = "On-device model"
    override val requiresInternet: Boolean = false

    override suspend fun analyze(
        imageUri: Uri,
        patient: PatientDetails
    ): Outcome<ScreeningResult> = Outcome.Failure(AppError.OfflineModelUnavailable)
}
