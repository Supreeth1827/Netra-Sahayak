package com.sih.netrasahayak.ui.preview

import android.net.Uri
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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sih.netrasahayak.ui.components.ImagePreviewCard
import com.sih.netrasahayak.ui.components.LoadingView
import com.sih.netrasahayak.ui.components.PrimaryButton
import com.sih.netrasahayak.ui.components.ScreenPadding
import com.sih.netrasahayak.ui.components.SecondaryButton
import com.sih.netrasahayak.ui.screening.AnalysisState

/**
 * Shows the captured or selected image and waits for an explicit decision.
 *
 * Nothing is uploaded until ANALYZE IMAGE is pressed. While a request is
 * running the buttons are replaced by a loading state, so the same image can
 * never be submitted twice.
 */
@Composable
fun ImagePreviewScreen(
    imageUri: Uri?,
    analysis: AnalysisState,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (analysis is AnalysisState.Analyzing) {
        LoadingView(
            title = "Analyzing retinal image...",
            subtitle = "Please wait.",
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 20.dp)
    ) {
        Text(
            text = "Check the Image",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Make sure the eye is clear and in focus before analysing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        ImagePreviewCard(model = imageUri)

        if (analysis is AnalysisState.Error) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = analysis.error.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            text = if (analysis is AnalysisState.Error) "TRY AGAIN" else "ANALYZE IMAGE",
            icon = Icons.Filled.Science,
            enabled = imageUri != null,
            onClick = onAnalyze
        )

        Spacer(Modifier.height(16.dp))

        SecondaryButton(
            text = "RETAKE",
            icon = Icons.Filled.PhotoCamera,
            onClick = onRetake
        )

        Spacer(Modifier.height(16.dp))

        SecondaryButton(
            text = "CHOOSE ANOTHER",
            icon = Icons.Filled.PhotoLibrary,
            onClick = onChooseAnother
        )

        Spacer(Modifier.height(24.dp))
    }
}
