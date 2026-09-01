package com.sih.netrasahayak.model

/**
 * The minimum information needed for a screening record.
 * Entered entirely offline - nothing here requires a network connection.
 */
data class PatientDetails(
    val patientId: String,
    val age: Int,
    val gender: Gender? = null,
    val diabetesDurationYears: Int? = null
)

enum class Gender(val label: String, val apiValue: String) {
    FEMALE("Female", "female"),
    MALE("Male", "male"),
    OTHER("Other", "other");

    companion object {
        fun fromApiValue(value: String?): Gender? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/** Result of validating the patient form. Used to show field-level messages. */
data class PatientFormErrors(
    val patientIdError: String? = null,
    val ageError: String? = null,
    val diabetesDurationError: String? = null
) {
    val hasErrors: Boolean
        get() = patientIdError != null || ageError != null || diabetesDurationError != null
}

object PatientValidator {

    const val MIN_AGE = 1
    const val MAX_AGE = 120

    fun validate(
        patientId: String,
        ageText: String,
        diabetesDurationText: String
    ): PatientFormErrors {
        val idError = when {
            patientId.isBlank() -> "Please enter a Patient ID."
            patientId.trim().length < 2 -> "Patient ID is too short."
            else -> null
        }

        val age = ageText.trim().toIntOrNull()
        val ageError = when {
            ageText.isBlank() -> "Please enter the patient's age."
            age == null -> "Age must be a number."
            age < MIN_AGE || age > MAX_AGE -> "Age must be between $MIN_AGE and $MAX_AGE."
            else -> null
        }

        val duration = diabetesDurationText.trim().toIntOrNull()
        val durationError = when {
            diabetesDurationText.isBlank() -> null // optional field
            duration == null -> "Years must be a number."
            duration < 0 || duration > MAX_AGE -> "Please enter a realistic number of years."
            age != null && duration > age -> "Cannot be more than the patient's age."
            else -> null
        }

        return PatientFormErrors(idError, ageError, durationError)
    }
}
