package com.sih.netrasahayak.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.model.Outcome
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * CameraX implementation backed by the phone's normal rear camera.
 *
 * No fundus-camera SDK is required or assumed. A future
 * FundusCameraController can replace this class behind
 * [RetinalCameraController] without touching the screens.
 */
class PhoneCameraController(private val context: Context) : RetinalCameraController {

    private val tag = "PhoneCameraController"

    override val deviceName: String = "Phone camera"

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    override suspend fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Outcome<Unit> {
        return try {
            val provider = ProcessCameraProvider.getInstance(context).awaitOnMain()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Log.w(tag, "Unable to start camera", t)
            Outcome.Failure(AppError.CameraUnavailable)
        }
    }

    override fun capture(outputFile: File, onResult: (Outcome<Uri>) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onResult(Outcome.Failure(AppError.CameraUnavailable))
            return
        }

        outputFile.parentFile?.mkdirs()
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        try {
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        onResult(Outcome.Success(output.savedUri ?: Uri.fromFile(outputFile)))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.w(tag, "Capture failed", exception)
                        onResult(Outcome.Failure(AppError.CameraUnavailable))
                    }
                }
            )
        } catch (t: Throwable) {
            Log.w(tag, "Capture threw", t)
            onResult(Outcome.Failure(AppError.CameraUnavailable))
        }
    }

    override fun stop() {
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        imageCapture = null
    }

    /** Bridges CameraX's ListenableFuture into a coroutine without extra libraries. */
    private suspend fun <T> ListenableFuture<T>.awaitOnMain(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (t: Throwable) {
                        continuation.cancel(t)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
}
