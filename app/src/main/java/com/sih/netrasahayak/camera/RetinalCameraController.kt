package com.sih.netrasahayak.camera

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.sih.netrasahayak.model.Outcome
import java.io.File

/**
 * How the app gets a retinal image out of a camera.
 *
 * Today the only implementation is [PhoneCameraController], which uses CameraX
 * and the phone's ordinary rear camera - the project has no dedicated fundus
 * camera. When one becomes available, add a FundusCameraController that talks to
 * the vendor SDK and implements this same interface; nothing in the UI layer
 * needs to change.
 */
interface RetinalCameraController {

    /** Human-readable name of the capture device, shown on the camera screen. */
    val deviceName: String

    /** Starts the preview. Returns a failure if the camera cannot be opened. */
    suspend fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView): Outcome<Unit>

    /** Takes one photo into [outputFile] and reports the resulting file Uri. */
    fun capture(outputFile: File, onResult: (Outcome<Uri>) -> Unit)

    /** Releases the camera. Safe to call more than once. */
    fun stop()
}
