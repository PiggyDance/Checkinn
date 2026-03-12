package io.piggydance.checkinn

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekOfYear
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

private fun formatter(): NSDateFormatter {
    val f = NSDateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f
}

private fun parseDate(dateString: String): NSDate {
    return formatter().dateFromString(dateString)!!
}

private fun calendar(): NSCalendar {
    val cal = NSCalendar.currentCalendar
    cal.firstWeekday = 2u // Monday = 2 in NSCalendar
    return cal
}

actual fun getWeekDates(dateString: String): List<String> {
    val cal = calendar()
    val fmt = formatter()
    var date = parseDate(dateString)

    // 找到本周一
    val weekday = cal.component(NSCalendarUnitWeekday, fromDate = date)
    // NSCalendar: Sunday=1, Monday=2, ... Saturday=7
    // 偏移到周一
    val daysToMonday = if (weekday.toInt() == 1) -6 else 2 - weekday.toInt()
    date = cal.dateByAddingUnit(NSCalendarUnitDay, daysToMonday.toLong(), toDate = date, options = 0u)!!

    return (0 until 7).map { i ->
        val d = fmt.stringFromDate(
            cal.dateByAddingUnit(NSCalendarUnitDay, i.toLong(), toDate = date, options = 0u)!!
        )
        d
    }
}

actual fun getMonthDates(dateString: String): List<String> {
    val cal = calendar()
    val fmt = formatter()
    val date = parseDate(dateString)
    val comps = cal.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = date)
    comps.setDay(1)
    val firstDay = cal.dateFromComponents(comps)!!
    val range = cal.rangeOfUnit(NSCalendarUnitDay, inUnit = NSCalendarUnitMonth, forDate = date)
    val maxDay = range.length.toInt()

    return (0 until maxDay).map { i ->
        fmt.stringFromDate(
            cal.dateByAddingUnit(NSCalendarUnitDay, i.toLong(), toDate = firstDay, options = 0u)!!
        )
    }
}

actual fun offsetWeek(dateString: String, offset: Int): String {
    val cal = calendar()
    val date = parseDate(dateString)
    val newDate = cal.dateByAddingUnit(NSCalendarUnitWeekOfYear, offset.toLong(), toDate = date, options = 0u)!!
    return formatter().stringFromDate(newDate)
}

actual fun offsetMonth(dateString: String, offset: Int): String {
    val cal = calendar()
    val date = parseDate(dateString)
    val newDate = cal.dateByAddingUnit(NSCalendarUnitMonth, offset.toLong(), toDate = date, options = 0u)!!
    return formatter().stringFromDate(newDate)
}

actual fun dayOfWeekShort(dateString: String): String {
    val cal = calendar()
    val date = parseDate(dateString)
    val weekday = cal.component(NSCalendarUnitWeekday, fromDate = date).toInt()
    return when (weekday) {
        2 -> "一"
        3 -> "二"
        4 -> "三"
        5 -> "四"
        6 -> "五"
        7 -> "六"
        1 -> "日"
        else -> ""
    }
}

actual fun dayOfMonth(dateString: String): Int {
    val cal = calendar()
    val date = parseDate(dateString)
    return cal.component(NSCalendarUnitDay, fromDate = date).toInt()
}

actual fun formatShortDate(dateString: String): String {
    val cal = calendar()
    val date = parseDate(dateString)
    val m = cal.component(NSCalendarUnitMonth, fromDate = date)
    val d = cal.component(NSCalendarUnitDay, fromDate = date)
    return "${m}月${d}日"
}

actual fun formatYearMonth(dateString: String): String {
    val cal = calendar()
    val date = parseDate(dateString)
    val y = cal.component(NSCalendarUnitYear, fromDate = date)
    val m = cal.component(NSCalendarUnitMonth, fromDate = date)
    return "${y}年${m}月"
}

actual fun formatWeekHeader(startDate: String, endDate: String): String {
    val cal = calendar()
    val s = parseDate(startDate)
    val e = parseDate(endDate)
    val sm = cal.component(NSCalendarUnitMonth, fromDate = s)
    val sd = cal.component(NSCalendarUnitDay, fromDate = s)
    val em = cal.component(NSCalendarUnitMonth, fromDate = e)
    val ed = cal.component(NSCalendarUnitDay, fromDate = e)
    return "${sm}/${sd} - ${em}/${ed}"
}
