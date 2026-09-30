package io.piggydance.checkinn

/** Work sessions stay on their clock-in date, including sessions that cross midnight. */
class CheckinnSessions(private val storage: CheckinnStorageInterface) {
    fun activeRecord(): DayRecord? = storage.getAllRecordDates()
        .sortedDescending()
        .asSequence()
        .map(storage::loadDayRecord)
        .firstOrNull { it.hasActiveSession }

    fun currentRecord(today: String): DayRecord = activeRecord() ?: storage.loadDayRecord(today)

    fun clockIn(today: String, now: Long): CheckResult {
        activeRecord()?.activeSession?.let {
            return CheckResult.AlreadyClockedIn(it.clockInTime)
        }
        val record = storage.loadDayRecord(today)
        storage.saveDayRecord(record.copy(
            sessions = record.sessions + WorkSession(clockInTime = now, clockOutTime = null),
        ))
        return CheckResult.ClockInSuccess(now)
    }

    fun clockOut(now: Long): CheckResult {
        val record = activeRecord() ?: return CheckResult.NotClockedIn
        val active = record.activeSession ?: return CheckResult.NotClockedIn
        val updatedRecord = record.copy(sessions = record.sessions.map { session ->
            if (session.clockOutTime == null) session.copy(clockOutTime = now) else session
        })
        storage.saveDayRecord(updatedRecord)
        return CheckResult.ClockOutSuccess(now - active.clockInTime, updatedRecord.totalDurationMs, record.date)
    }
}

/** Keeps the existing SharedPreferences format: clockIn,clockOut;clockIn, */
object CheckinnRecordCodec {
    fun encode(record: DayRecord): String = record.sessions.joinToString(";") {
        "${it.clockInTime},${it.clockOutTime ?: ""}"
    }

    fun decode(date: String, value: String?): DayRecord {
        if (value.isNullOrBlank()) return DayRecord(date)
        val sessions = value.split(";").mapNotNull { part ->
            val fields = part.split(",")
            val clockIn = fields.firstOrNull()?.toLongOrNull() ?: return@mapNotNull null
            WorkSession(clockIn, fields.getOrNull(1)?.toLongOrNull())
        }
        return DayRecord(date, sessions)
    }
}
