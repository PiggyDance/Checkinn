package io.piggydance.checkinn

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun parseDate(dateString: String): Calendar {
    val cal = Calendar.getInstance()
    cal.time = sdf.parse(dateString)!!
    cal.firstDayOfWeek = Calendar.MONDAY
    return cal
}

actual fun getWeekDates(dateString: String): List<String> {
    val cal = parseDate(dateString)
    // 跳到本周一
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    return (0 until 7).map { i ->
        val d = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        d
    }
}

actual fun getMonthDates(dateString: String): List<String> {
    val cal = parseDate(dateString)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (0 until maxDay).map { i ->
        val d = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        d
    }
}

actual fun offsetWeek(dateString: String, offset: Int): String {
    val cal = parseDate(dateString)
    cal.add(Calendar.WEEK_OF_YEAR, offset)
    return sdf.format(cal.time)
}

actual fun offsetMonth(dateString: String, offset: Int): String {
    val cal = parseDate(dateString)
    cal.add(Calendar.MONTH, offset)
    return sdf.format(cal.time)
}

actual fun dayOfWeekShort(dateString: String): String {
    val cal = parseDate(dateString)
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "一"
        Calendar.TUESDAY -> "二"
        Calendar.WEDNESDAY -> "三"
        Calendar.THURSDAY -> "四"
        Calendar.FRIDAY -> "五"
        Calendar.SATURDAY -> "六"
        Calendar.SUNDAY -> "日"
        else -> ""
    }
}

actual fun dayOfMonth(dateString: String): Int {
    val cal = parseDate(dateString)
    return cal.get(Calendar.DAY_OF_MONTH)
}

actual fun formatShortDate(dateString: String): String {
    val cal = parseDate(dateString)
    return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
}

actual fun formatYearMonth(dateString: String): String {
    val cal = parseDate(dateString)
    return "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
}

actual fun formatWeekHeader(startDate: String, endDate: String): String {
    val s = parseDate(startDate)
    val e = parseDate(endDate)
    return "${s.get(Calendar.MONTH) + 1}/${s.get(Calendar.DAY_OF_MONTH)} - ${e.get(Calendar.MONTH) + 1}/${e.get(Calendar.DAY_OF_MONTH)}"
}
