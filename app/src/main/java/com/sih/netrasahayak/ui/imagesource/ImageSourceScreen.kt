package com.sih.netrasahayak.ui.imagesource

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sih.netrasahayak.camera.ImageUtils
import com.sih.netrasahayak.model.AppError
import com.sih.netrasahayak.ui.components.DisclaimerBanner
import com.sih.netrasahayak.ui.components.PrimaryButton
import com.sih.netrasahayak.ui.components.ScreenPadding
import kotlinx.coroutines.launch

/**
 * "Provide Retinal Image" - the fork between camera and gallery.
 *
 * Camera permission is asked for ONLY when TAKE PHOTO is tapped, never on
 * screen entry. Gallery uses the system photo picker, which needs no storage
 * permission at all.
 */
@Composable
fun ImageSourceScreen(
    onOpenCamera: () -> Unit,
    onImageSelected: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var checkingImage by remember { mutableStateOf(false) }

    // Modern photo picker. Falls back to the document picker on older devices,
    // and grants read access without any storage permission.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            errorMessage = AppError.NoImageSelected.message
        } else {
            checkingImage = true
            scope.launch {
                val readable = ImageUtils.isReadableImage(context, uri)
                checkingImage = false
                if (readable) {
                    errorMessage = null
                    onImageSelected(uri)
                } else {
                    errorMessage = AppError.InvalidImage.message
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            errorMessage = null
            onOpenCamera()
        } else {
            errorMessage = AppError.CameraPermissionDenied.message
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Provide Retinal Image",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Capture a new image or select an existing retinal image.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        PrimaryButton(
            text = "TAKE PHOTO",
            icon = Icons.Filled.PhotoCamera,
            enabled = !checkingImage,
            onClick = {
                errorMessage = null
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    onOpenCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "CHOOSE FROM GALLERY",
            icon = Icons.Filled.PhotoLibrary,
            enabled = !checkingImage,
            onClick = {
                errorMessage = null
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = "This version uses the phone's normal camera. A dedicated fundus " +
                "camera can be connected later without changing this screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        DisclaimerBanner()

        Spacer(Modifier.height(24.dp))
    }
}
