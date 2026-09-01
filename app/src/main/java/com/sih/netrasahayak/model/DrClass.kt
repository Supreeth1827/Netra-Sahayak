package com.sih.netrasahayak.model

/**
 * The five diabetic-retinopathy grades the screening model can report.
 *
 * [apiLabel] is what the FastAPI backend is expected to send in the
 * "prediction" field. Parsing is deliberately forgiving (see [fromApi]) so a
 * small wording change on the backend does not break the app.
 */
enum class DrClass(
    val apiLabel: String,
    val displayName: String,
    val shortName: String,
    /** 0 = no disease ... 4 = most advanced. Used for colour and ordering only. */
    val severity: Int,
    /** Plain-language advice shown when the backend does not send its own. */
    val defaultRecommendation: String
) {
    NO_DR(
        apiLabel = "No Diabetic Retinopathy",
        displayName = "No Diabetic Retinopathy",
        shortName = "No DR",
        severity = 0,
        defaultRecommendation = "No signs found. Repeat screening after one year."
    ),
    MILD(
        apiLabel = "Mild Diabetic Retinopathy",
        displayName = "Mild Diabetic Retinopathy",
        shortName = "Mild DR",
        severity = 1,
        defaultRecommendation = "Repeat screening in 6 to 12 months. Advise blood sugar control."
    ),
    MODERATE(
        apiLabel = "Moderate Diabetic Retinopathy",
        displayName = "Moderate Diabetic Retinopathy",
        shortName = "Moderate DR",
        severity = 2,
        defaultRecommendation = "Clinical evaluation by an ophthalmologist is recommended."
    ),
    SEVERE(
        apiLabel = "Severe Diabetic Retinopathy",
        displayName = "Severe Diabetic Retinopathy",
        shortName = "Severe DR",
        severity = 3,
        defaultRecommendation = "Referral recommended. Please arrange an eye specialist visit soon."
    ),
    PROLIFERATIVE(
        apiLabel = "Proliferative Diabetic Retinopathy",
        displayName = "Proliferative Diabetic Retinopathy",
        shortName = "Proliferative DR",
        severity = 4,
        defaultRecommendation = "Urgent referral recommended. Please arrange an eye specialist visit."
    );

    /** True when the screening suggests the patient should see a specialist. */
    val referralSuggested: Boolean get() = severity >= 2

    companion object {

        /**
         * Maps a raw backend string onto a known grade.
         * Accepts "Moderate Diabetic Retinopathy", "moderate_dr", "Moderate DR",
         * "2" and similar. Returns null when nothing matches, which the caller
         * treats as an invalid API response.
         */
        fun fromApi(raw: String?): DrClass? {
            if (raw.isNullOrBlank()) return null
            val text = raw.trim().lowercase().replace('_', ' ').replace('-', ' ')

            // A bare severity index is also accepted ("0".."4").
            text.toIntOrNull()?.let { index ->
                return entries.firstOrNull { it.severity == index }
            }

            return when {
                text.contains("prolifer") -> PROLIFERATIVE
                text.contains("severe") -> SEVERE
                text.contains("moderate") -> MODERATE
                text.contains("mild") -> MILD
                text.startsWith("no") || text.contains("no dr") ||
                    text.contains("normal") || text.contains("no diabetic") -> NO_DR
                else -> null
            }
        }
    }
}
