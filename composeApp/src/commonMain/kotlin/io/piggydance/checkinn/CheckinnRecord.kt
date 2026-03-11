package io.piggydance.checkinn

/**
 * 表示一段工作时间 (上班打卡 -> 下班打卡)
 */
data class WorkSession(
    val clockInTime: Long,   // 上班打卡时间戳 (毫秒)
    val clockOutTime: Long?, // 下班打卡时间戳 (毫秒), null 表示还没下班
) {
    /** 本段工作时长(毫秒), 未打下班卡则用当前时间计算 */
    val durationMs: Long
        get() = (clockOutTime ?: currentTimeMillis()) - clockInTime
}

/**
 * 一天的打卡记录, 支持多段累计
 */
data class DayRecord(
    val date: String,                     // 日期 yyyy-MM-dd
    val sessions: List<WorkSession> = emptyList(),
) {
    /** 今天累计工作时长(毫秒) */
    val totalDurationMs: Long
        get() = sessions.sumOf { it.durationMs }

    /** 是否有正在进行中的工作段(上了班还没下班) */
    val hasActiveSession: Boolean
        get() = sessions.any { it.clockOutTime == null }

    /** 当前活跃的 session */
    val activeSession: WorkSession?
        get() = sessions.firstOrNull { it.clockOutTime == null }
}

/** NFC 贴纸的场景类型 */
enum class NfcScene(val key: String) {
    CLOCK_IN("clock_in"),
    CLOCK_OUT("clock_out");

    companion object {
        fun fromKey(key: String): NfcScene? = entries.firstOrNull { it.key == key }
    }
}

/** 打卡结果 */
sealed class CheckResult {
    data class ClockInSuccess(val time: Long) : CheckResult()
    data class ClockOutSuccess(val sessionDuration: Long, val totalDuration: Long) : CheckResult()
    data class AlreadyClockedIn(val existingTime: Long) : CheckResult()
    data object NotClockedIn : CheckResult()
}

expect fun currentTimeMillis(): Long
