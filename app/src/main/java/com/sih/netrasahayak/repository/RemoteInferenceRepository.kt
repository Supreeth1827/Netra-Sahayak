package com.sih.netrasahayak.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sih.netrasahayak.camera.ImageUtils
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.DrClass
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult
import com.sih.netrasahayak.network.ConnectivityObserver
import com.sih.netrasahayak.network.NetraApiService
import com.sih.netrasahayak.network.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * PRODUCTION path: uploads the retinal image to the FastAPI backend and turns
 * its JSON reply into a [ScreeningResult].
 *
 * Request : multipart/form-data -> POST {BASE_URL}predict
 *           image (file), patient_id, age, gender, diabetes_duration
 * Response: { prediction, confidence, heatmap_url, recommendation }
 *
 * Every failure is mapped to a plain-language [AppError]; no exception ever
 * reaches the UI.
 */
class RemoteInferenceRepository(
    private val context: Context,
    private val api: NetraApiService,
    private val connectivity: ConnectivityObserver
) : InferenceRepository {

    private val tag = "RemoteInference"

    override val sourceName: String = "Screening server"
    override val requiresInternet: Boolean = true

    override suspend fun analyze(
        imageUri: Uri,
        patient: PatientDetails
    ): Outcome<ScreeningResult> = withContext(Dispatchers.IO) {

        if (!connectivity.isOnline()) return@withContext Outcome.Failure(AppError.NoInternet)

        // 1. Shrink / rotate / compress a copy of the image for upload.
        val uploadFile: File = when (val prepared = ImageUtils.prepareForUpload(context, imageUri)) {
            is Outcome.Failure -> return@withContext prepared
            is Outcome.Success -> prepared.data
        }

        try {
            val imagePart = MultipartBody.Part.createFormData(
                name = "image",
                filename = uploadFile.name,
                body = uploadFile.asRequestBody("image/jpeg".toMediaType())
            )

            val response = api.predict(
                image = imagePart,
                patientId = patient.patientId.toFormPart(),
                age = patient.age.toString().toFormPart(),
                gender = patient.gender?.apiValue?.toFormPart(),
                diabetesDuration = patient.diabetesDurationYears?.toString()?.toFormPart()
            )

            if (!response.isSuccessful) {
                return@withContext Outcome.Failure(AppError.ServerError(response.code()))
            }

            val body = response.body()
                ?: return@withContext Outcome.Failure(AppError.InvalidResponse)

            val drClass = DrClass.fromApi(body.prediction)
                ?: return@withContext Outcome.Failure(AppError.InvalidResponse)

            val confidence = body.confidence?.let { raw ->
                // Accept either 0.91 or 91.
                if (raw > 1f) raw / 100f else raw
            }?.coerceIn(0f, 1f) ?: return@withContext Outcome.Failure(AppError.InvalidResponse)

            Outcome.Success(
                ScreeningResult(
                    drClass = drClass,
                    rawPrediction = body.prediction.orEmpty().ifBlank { drClass.displayName },
                    confidence = confidence,
                    heatmapUrl = RetrofitProvider.resolveUrl(body.heatmapUrl),
                    recommendation = body.recommendation?.takeIf { it.isNotBlank() }
                        ?: drClass.defaultRecommendation
                )
            )
        } catch (e: UnknownHostException) {
            Log.w(tag, "Host not reachable", e)
            Outcome.Failure(AppError.ServerUnreachable)
        } catch (e: ConnectException) {
            Log.w(tag, "Connection refused", e)
            Outcome.Failure(AppError.ServerUnreachable)
        } catch (e: SocketTimeoutException) {
            Log.w(tag, "Timed out", e)
            Outcome.Failure(AppError.Timeout)
        } catch (e: HttpException) {
            Log.w(tag, "HTTP error", e)
            Outcome.Failure(AppError.ServerError(e.code()))
        } catch (e: IOException) {
            Log.w(tag, "Upload failed", e)
            Outcome.Failure(AppError.UploadFailed)
        } catch (t: Throwable) {
            Log.w(tag, "Unexpected failure during analysis", t)
            Outcome.Failure(AppError.Unknown(t.message))
        } finally {
            // The upload copy is temporary; the original stays untouched.
            runCatching { uploadFile.delete() }
        }
    }

    private fun String.toFormPart(): RequestBody =
        toRequestBody("text/plain".toMediaType())
}
