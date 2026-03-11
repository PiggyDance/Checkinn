package io.piggydance.checkinn

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun formatTime(timestampMs: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMs / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    return formatter.stringFromDate(date)
}
