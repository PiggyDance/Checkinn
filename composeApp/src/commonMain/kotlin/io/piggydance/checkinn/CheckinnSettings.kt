package io.piggydance.checkinn

/**
 * 打卡设置
 */
data class CheckinnSettings(
    /** 每日工作目标时长（小时），范围 0-12 */
    val dailyGoalHours: Int = 8,
    /** 工作日设置，周一到周日分别对应 0-6 */
    val workDays: Set<Int> = setOf(0, 1, 2, 3, 4), // 默认周一到周五
)

/**
 * 平台无关的设置存储接口
 */
interface CheckinnSettingsStorage {
    fun saveSettings(settings: CheckinnSettings)
    fun loadSettings(): CheckinnSettings
}

/**
 * 获取星期几的显示名称
 */
fun getDayName(dayIndex: Int, strings: StringResources): String {
    return when (dayIndex) {
        0 -> strings.monday()
        1 -> strings.tuesday()
        2 -> strings.wednesday()
        3 -> strings.thursday()
        4 -> strings.friday()
        5 -> strings.saturday()
        6 -> strings.sunday()
        else -> ""
    }
}

/**
 * 获取星期几的短名称
 */
fun getDayShortName(dayIndex: Int, strings: StringResources): String {
    return when (dayIndex) {
        0 -> strings.mondayShort()
        1 -> strings.tuesdayShort()
        2 -> strings.wednesdayShort()
        3 -> strings.thursdayShort()
        4 -> strings.fridayShort()
        5 -> strings.saturdayShort()
        6 -> strings.sundayShort()
        else -> ""
    }
}
