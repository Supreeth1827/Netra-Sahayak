package com.sih.netrasahayak.ui.theme

import androidx.compose.ui.graphics.Color
import com.sih.netrasahayak.model.DrClass

// Calm clinical teal, high contrast on white. Chosen for outdoor readability.
val Teal700 = Color(0xFF00695C)
val Teal600 = Color(0xFF00796B)
val Teal100 = Color(0xFFB2DFDB)
val Teal50 = Color(0xFFE0F2F1)

val Slate900 = Color(0xFF10231F)
val Slate700 = Color(0xFF37474F)
val Slate300 = Color(0xFFCFD8DC)
val Surface0 = Color(0xFFFFFBFF)
val SurfaceMuted = Color(0xFFF1F4F3)

val Danger = Color(0xFFB3261E)
val DangerContainer = Color(0xFFFCE8E6)

// Severity palette used by the result and history screens.
val SeverityNone = Color(0xFF2E7D32)
val SeverityMild = Color(0xFF827717)
val SeverityModerate = Color(0xFFE65100)
val SeveritySevere = Color(0xFFC62828)
val SeverityProliferative = Color(0xFF8E0000)

val SeverityNoneContainer = Color(0xFFE6F4E7)
val SeverityMildContainer = Color(0xFFF7F5DE)
val SeverityModerateContainer = Color(0xFFFDECDD)
val SeveritySevereContainer = Color(0xFFFBE3E3)
val SeverityProliferativeContainer = Color(0xFFF6DADA)

fun DrClass.accentColor(): Color = when (this) {
    DrClass.NO_DR -> SeverityNone
    DrClass.MILD -> SeverityMild
    DrClass.MODERATE -> SeverityModerate
    DrClass.SEVERE -> SeveritySevere
    DrClass.PROLIFERATIVE -> SeverityProliferative
}

fun DrClass.containerColor(): Color = when (this) {
    DrClass.NO_DR -> SeverityNoneContainer
    DrClass.MILD -> SeverityMildContainer
    DrClass.MODERATE -> SeverityModerateContainer
    DrClass.SEVERE -> SeveritySevereContainer
    DrClass.PROLIFERATIVE -> SeverityProliferativeContainer
}
