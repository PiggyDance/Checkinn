package io.piggydance.checkinn

/**
 * 跨平台日期工具.
 * 所有日期格式统一使用 "yyyy-MM-dd".
 */

/** 获取包含指定日期的那一周的所有日期 (周一~周日) */
expect fun getWeekDates(dateString: String): List<String>

/** 获取指定日期所在月份的所有日期 */
expect fun getMonthDates(dateString: String): List<String>

/** 偏移周: offset=-1 上一周, offset=1 下一周 */
expect fun offsetWeek(dateString: String, offset: Int): String

/** 偏移月: offset=-1 上月, offset=1 下月 */
expect fun offsetMonth(dateString: String, offset: Int): String

/** 获取星期几的中文缩写, 0=周一 ... 6=周日 */
expect fun dayOfWeekShort(dateString: String): String

/** 获取日期中的"日" (1~31) */
expect fun dayOfMonth(dateString: String): Int

/** 格式化为 "M月d日" */
expect fun formatShortDate(dateString: String): String

/** 格式化为 "yyyy年M月" */
expect fun formatYearMonth(dateString: String): String

/** 格式化为 "M/d 周X" */
expect fun formatWeekHeader(startDate: String, endDate: String): String
