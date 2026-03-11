package io.piggydance.checkinn

import android.content.Context
import android.content.SharedPreferences

/**
 * 基于 SharedPreferences 的打卡记录持久化存储.
 * 保证 App 被杀掉后依然能恢复上班打卡时间.
 *
 * 存储格式:
 * - "sessions_{date}" -> "clockIn1,clockOut1;clockIn2,clockOut2;clockIn3,"
 *   每段用 ; 分隔, 每段内 clockIn,clockOut 用 , 分隔, 未下班的 clockOut 为空
 */
class CheckinnStorage(context: Context) : CheckinnStorageInterface {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("checkinn_records", Context.MODE_PRIVATE)

    override fun saveDayRecord(record: DayRecord) {
        val sessionsStr = record.sessions.joinToString(";") { session ->
            "${session.clockInTime},${session.clockOutTime ?: ""}"
        }
        prefs.edit()
            .putString("sessions_${record.date}", sessionsStr)
            .apply()
    }

    override fun loadDayRecord(date: String): DayRecord {
        val sessionsStr = prefs.getString("sessions_$date", null)
            ?: return DayRecord(date = date)

        if (sessionsStr.isBlank()) return DayRecord(date = date)

        val sessions = sessionsStr.split(";").mapNotNull { part ->
            val parts = part.split(",")
            if (parts.isEmpty() || parts[0].isBlank()) return@mapNotNull null
            val clockIn = parts[0].toLongOrNull() ?: return@mapNotNull null
            val clockOut = parts.getOrNull(1)?.toLongOrNull()
            WorkSession(clockInTime = clockIn, clockOutTime = clockOut)
        }

        return DayRecord(date = date, sessions = sessions)
    }

    override fun getAllRecordDates(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith("sessions_") }
            .map { it.removePrefix("sessions_") }
            .sorted()
    }
}
