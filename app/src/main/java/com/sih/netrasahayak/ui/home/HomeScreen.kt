package com.sih.netrasahayak.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sih.netrasahayak.ui.components.DisclaimerBanner
import com.sih.netrasahayak.ui.components.PrimaryButton
import com.sih.netrasahayak.ui.components.ScreenPadding
import com.sih.netrasahayak.ui.components.SecondaryButton

/**
 * Landing screen. Three actions, nothing else - the first thing an ASHA worker
 * sees must be obvious.
 */
@Composable
fun HomeScreen(
    onStartScreening: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Netra Sahayak",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "AI-Assisted Diabetic Retinopathy Screening",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        PrimaryButton(
            text = "START NEW SCREENING",
            icon = Icons.Filled.RemoveRedEye,
            onClick = onStartScreening
        )

        Spacer(Modifier.height(20.dp))

        SecondaryButton(
            text = "SCREENING HISTORY",
            icon = Icons.Filled.History,
            onClick = onOpenHistory
        )

        Spacer(Modifier.height(20.dp))

        SecondaryButton(
            text = "SYNC DATA",
            icon = Icons.Filled.CloudSync,
            onClick = onOpenSync
        )

        Spacer(Modifier.height(40.dp))

        DisclaimerBanner()
    }
}
