package com.sih.netrasahayak.network

import com.sih.netrasahayak.network.dto.PredictionResponseDto
import com.sih.netrasahayak.network.dto.ScreeningUploadDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * The FastAPI contract, as seen from Android.
 *
 * FastAPI side (for reference, implemented separately in Python):
 *
 *   @app.post("/predict")
 *   async def predict(
 *       image: UploadFile = File(...),
 *       patient_id: str | None = Form(None),
 *       age: int | None = Form(None),
 *       gender: str | None = Form(None),
 *       diabetes_duration: int | None = Form(None),
 *   ): ...
 */
interface NetraApiService {

    /**
     * Sends one retinal image (multipart/form-data) for EfficientNet inference
     * and Grad-CAM generation.
     *
     * The optional form fields are sent only when the ASHA worker supplied them;
     * Retrofit omits null parts entirely.
     */
    @Multipart
    @POST("predict")
    suspend fun predict(
        @Part image: MultipartBody.Part,
        @Part("patient_id") patientId: RequestBody? = null,
        @Part("age") age: RequestBody? = null,
        @Part("gender") gender: RequestBody? = null,
        @Part("diabetes_duration") diabetesDuration: RequestBody? = null
    ): Response<PredictionResponseDto>

    /**
     * Uploads one locally-stored screening record.
     *
     * Not yet available on the backend - see [ScreeningUploadDto]. The interface
     * exists so sync can be switched on without touching the UI or database.
     */
    @POST("screenings")
    suspend fun uploadScreening(@Body screening: ScreeningUploadDto): Response<Unit>
}
