package com.sih.netrasahayak.ui.navigation

/** Every destination in the app. Kept in one place so routes never drift. */
object Routes {

    const val HOME = "home"

    /** Nested graph: one ScreeningViewModel is shared by everything inside it. */
    const val SCREENING_GRAPH = "screening"
    const val PATIENT = "screening/patient"
    const val IMAGE_SOURCE = "screening/image_source"
    const val CAMERA = "screening/camera"
    const val PREVIEW = "screening/preview"
    const val RESULT = "screening/result"

    const val HISTORY = "history"
    const val HISTORY_DETAIL_ARG = "screeningId"
    const val HISTORY_DETAIL = "history/{$HISTORY_DETAIL_ARG}"
    fun historyDetail(id: Long): String = "history/$id"

    const val SYNC = "sync"
}
