package com.sih.netrasahayak.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.sih.netrasahayak.model.Gender
import com.sih.netrasahayak.model.PatientFormErrors
import com.sih.netrasahayak.model.PatientValidator
import com.sih.netrasahayak.ui.components.PrimaryButton
import com.sih.netrasahayak.ui.components.ScreenPadding

/**
 * Collects the minimum patient information. Works entirely offline - nothing on
 * this screen touches the network.
 *
 * Typing is kept to a minimum: two short fields, and two optional ones where
 * gender is chosen by tapping.
 */
@Composable
fun PatientDetailsScreen(
    onContinue: (patientId: String, age: Int, gender: Gender?, diabetesYears: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var patientId by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }
    // Stored as the enum name so it survives process death without a custom Saver.
    var genderName by rememberSaveable { mutableStateOf<String?>(null) }
    val gender: Gender? = remember(genderName) {
        Gender.entries.firstOrNull { it.name == genderName }
    }
    var diabetesYears by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(PatientFormErrors()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 24.dp)
    ) {
        Text(
            text = "Patient Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Only what is needed for the screening record.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = patientId,
            onValueChange = {
                patientId = it
                if (errors.patientIdError != null) errors = errors.copy(patientIdError = null)
            },
            label = { Text("Patient ID", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            isError = errors.patientIdError != null,
            supportingText = errors.patientIdError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = ageText,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 3) {
                    ageText = input
                    if (errors.ageError != null) errors = errors.copy(ageError = null)
                }
            },
            label = { Text("Age (years)", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            isError = errors.ageError != null,
            supportingText = errors.ageError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Gender (optional)",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Gender.entries.forEach { option ->
                FilterChip(
                    selected = gender == option,
                    onClick = {
                        genderName = if (gender == option) null else option.name
                    },
                    label = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = diabetesYears,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 3) {
                    diabetesYears = input
                    if (errors.diabetesDurationError != null) {
                        errors = errors.copy(diabetesDurationError = null)
                    }
                }
            },
            label = {
                Text("Years with diabetes (optional)", style = MaterialTheme.typography.bodyLarge)
            },
            singleLine = true,
            isError = errors.diabetesDurationError != null,
            supportingText = errors.diabetesDurationError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(40.dp))

        PrimaryButton(
            text = "CONTINUE",
            onClick = {
                val validation = PatientValidator.validate(patientId, ageText, diabetesYears)
                errors = validation
                if (!validation.hasErrors) {
                    onContinue(
                        patientId.trim(),
                        ageText.trim().toInt(),
                        gender,
                        diabetesYears.trim().toIntOrNull()
                    )
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}
