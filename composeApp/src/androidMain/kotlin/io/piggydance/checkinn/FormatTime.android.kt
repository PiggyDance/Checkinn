package io.piggydance.checkinn

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}
