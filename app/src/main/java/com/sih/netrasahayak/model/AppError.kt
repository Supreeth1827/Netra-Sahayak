package com.sih.netrasahayak.model

/**
 * Every failure the user can see is one of these. Raw exceptions and stack
 * traces are never shown - only [message].
 */
sealed class AppError(val message: String) {

    data object NoImageSelected : AppError("No image selected.")

    data object CameraUnavailable : AppError("Unable to access camera.")

    data object CameraPermissionDenied :
        AppError("Camera permission is needed to take a photo. Please allow it in Settings.")

    data object InvalidImage : AppError("This file is not a valid image. Please choose another.")

    data object ImageTooLarge :
        AppError("This image is too large to process. Please use a smaller image.")

    data object NoInternet :
        AppError("No internet connection. Screening needs internet. Patient details and history still work offline.")

    data object Timeout : AppError("The screening server took too long to respond. Please try again.")

    data object ServerUnreachable : AppError("Unable to connect to the screening server. Please try again.")

    data class ServerError(val code: Int) :
        AppError("The screening server reported a problem (error $code). Please try again.")

    data object InvalidResponse :
        AppError("The screening server sent an unexpected reply. Please try again.")

    data object UploadFailed : AppError("Could not upload the image. Please try again.")

    data object StorageError : AppError("Could not save the screening on this phone.")

    data object OfflineModelUnavailable :
        AppError("Offline AI screening is not available in this version. Please connect to the internet.")

    data class Unknown(val details: String? = null) : AppError("Something went wrong. Please try again.")
}
