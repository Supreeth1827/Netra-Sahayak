package com.sih.netrasahayak.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sih.netrasahayak.database.ScreeningEntity
import com.sih.netrasahayak.model.DrClass
import com.sih.netrasahayak.model.Gender
import com.sih.netrasahayak.ui.components.DisclaimerBanner
import com.sih.netrasahayak.ui.components.ErrorView
import com.sih.netrasahayak.ui.components.LabelledSection
import com.sih.netrasahayak.ui.components.LoadingView
import com.sih.netrasahayak.ui.components.ResultCard
import com.sih.netrasahayak.ui.components.RetinalImageViewer
import com.sih.netrasahayak.ui.components.ScreenPadding
import com.sih.netrasahayak.ui.components.formatDateTime
import com.sih.netrasahayak.ui.components.imageModel

/**
 * One saved screening, re-opened from history. Reads only from Room, so it
 * works with no internet - except a heatmap that lives on the server, which
 * simply fails to load and leaves the ORIGINAL view usable.
 */
@Composable
fun HistoryDetailScreen(
    screening: ScreeningEntity?,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    if (loading) {
        LoadingView(title = "Opening screening...", modifier = modifier)
        return
    }

    if (screening == null) {
        ErrorView(
            message = "This screening could not be found on this phone.",
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val drClass = DrClass.fromApi(screening.prediction)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Patient",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = screening.patientId,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    LabelledSection(label = "Age", value = "${screening.age} years")
                    LabelledSection(
                        label = "Gender",
                        value = Gender.fromApiValue(screening.gender)?.label ?: "Not recorded"
                    )
                }

                Spacer(Modifier.height(16.dp))

                LabelledSection(
                    label = "Years with diabetes",
                    value = screening.diabetesDurationYears?.let { "$it" } ?: "Not recorded"
                )

                Spacer(Modifier.height(16.dp))

                LabelledSection(
                    label = "Screened on",
                    value = formatDateTime(screening.createdAt)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (drClass != null) {
            ResultCard(drClass = drClass, confidence = screening.confidence)
        } else {
            Text(
                text = screening.prediction,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Why this result?",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Highlighted regions indicate areas that contributed to the AI " +
                "model's prediction.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        RetinalImageViewer(
            originalModel = imageModel(screening.imagePath),
            heatmapModel = imageModel(screening.heatmapUrl)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Recommended Action",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = screening.recommendation,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(18.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        DisclaimerBanner()

        Spacer(Modifier.height(24.dp))
    }
}
