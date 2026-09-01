package com.sih.netrasahayak.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "31 Aug 2026" - short and unambiguous. */
fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

/** "31 Aug 2026, 10:45 AM" - used on the history detail screen. */
fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(epochMillis))

/**
 * Turns whatever we stored (http URL, content:// Uri, file:// Uri or a plain
 * absolute path) into something Coil can load.
 */
fun imageModel(pathOrUrl: String?): Any? = when {
    pathOrUrl.isNullOrBlank() -> null
    pathOrUrl.startsWith("http://", true) || pathOrUrl.startsWith("https://", true) -> pathOrUrl
    pathOrUrl.startsWith("content://", true) -> android.net.Uri.parse(pathOrUrl)
    pathOrUrl.startsWith("file://", true) -> android.net.Uri.parse(pathOrUrl)
    pathOrUrl.startsWith("/") -> java.io.File(pathOrUrl)
    else -> pathOrUrl
}
