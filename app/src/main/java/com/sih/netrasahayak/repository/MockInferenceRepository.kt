package com.sih.netrasahayak.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import com.sih.netrasahayak.model.DrClass
import com.sih.netrasahayak.model.Outcome
import com.sih.netrasahayak.model.PatientDetails
import com.sih.netrasahayak.model.ScreeningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/**
 * ============================== MOCK / DEMO ONLY ==============================
 *
 * Used when AppConfig.USE_MOCK_API is true, so the whole UI can be demonstrated
 * before the FastAPI + EfficientNet backend exists.
 *
 * It returns a plausible-looking grade, a confidence value and a PLACEHOLDER
 * heatmap that it draws on the phone (warm blobs painted over the retinal
 * image, the same shape a real Grad-CAM overlay has).
 *
 * NOTHING in this class is used in a release build - the release build type sets
 * USE_MOCK_API = false. No fake prediction logic exists anywhere in the
 * production path.
 * =============================================================================
 */
class MockInferenceRepository(private val context: Context) : InferenceRepository {

    private val tag = "MockInference"

    override val sourceName: String = "Demo mode (no server)"
    override val requiresInternet: Boolean = false

    override suspend fun analyze(
        imageUri: Uri,
        patient: PatientDetails
    ): Outcome<ScreeningResult> = withContext(Dispatchers.IO) {
        // Pretend the network round-trip and inference take a moment.
        delay(2200)

        val drClass = pickDemoClass(patient.patientId)
        val confidence = 0.78f + Random.nextFloat() * 0.20f

        val heatmap = runCatching { createPlaceholderHeatmap(imageUri) }
            .onFailure { Log.w(tag, "Could not draw placeholder heatmap", it) }
            .getOrNull()

        Outcome.Success(
            ScreeningResult(
                drClass = drClass,
                rawPrediction = drClass.apiLabel,
                confidence = confidence.coerceIn(0f, 1f),
                heatmapUrl = heatmap?.absolutePath,
                recommendation = drClass.defaultRecommendation
            )
        )
    }

    /**
     * Deterministic per patient ID so the same demo patient always shows the
     * same grade - handy when presenting the app.
     */
    private fun pickDemoClass(patientId: String): DrClass {
        val classes = DrClass.entries
        val index = (patientId.hashCode().toLong() and 0x7fffffff) % classes.size
        return classes[index.toInt()]
    }

    /** Paints warm activation blobs over the retinal image. Demo artefact only. */
    private fun createPlaceholderHeatmap(imageUri: Uri): File? {
        // inJustDecodeBounds makes decodeStream return null by design; check the
        // stream itself, then read the size out of `bounds`.
        val boundsStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, bounds.outWidth / 720)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val source = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val canvasBitmap = source.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        source.recycle()

        val canvas = Canvas(canvasBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(canvasBitmap.width * 31 + canvasBitmap.height)
        val blobCount = 3 + random.nextInt(3)

        repeat(blobCount) {
            val cx = canvasBitmap.width * (0.25f + random.nextFloat() * 0.5f)
            val cy = canvasBitmap.height * (0.25f + random.nextFloat() * 0.5f)
            val radius = canvasBitmap.width * (0.10f + random.nextFloat() * 0.12f)
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.argb(210, 255, 40, 0),
                    Color.argb(150, 255, 190, 0),
                    Color.argb(0, 255, 255, 0)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        val dir = File(context.cacheDir, "mock_heatmaps").apply { mkdirs() }
        val target = File(dir, "heatmap_${System.currentTimeMillis()}.png")
        FileOutputStream(target).use { out ->
            canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        canvasBitmap.recycle()
        return target
    }
}
