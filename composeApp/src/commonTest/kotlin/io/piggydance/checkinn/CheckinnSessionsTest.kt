package io.piggydance.checkinn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CheckinnSessionsTest {
    private val hour = 60L * 60 * 1000

    @Test
    fun overnightClockOutPreservesTheFullSessionOnItsClockInDate() {
        val storage = SerializedStorage()
        val sessions = CheckinnSessions(storage)
        val clockIn = 23 * hour + 45 * 60_000L
        val clockOut = 25 * hour + 15 * 60_000L

        assertIs<CheckResult.ClockInSuccess>(sessions.clockIn("2026-09-30", clockIn))
        val result = assertIs<CheckResult.ClockOutSuccess>(sessions.clockOut(clockOut))

        assertEquals(90 * 60_000L, result.sessionDuration)
        assertEquals("2026-09-30", result.recordDate)
        assertEquals(result.sessionDuration, result.totalDuration)
        assertEquals(listOf(WorkSession(clockIn, clockOut)), storage.loadDayRecord("2026-09-30").sessions)
        assertTrue(storage.loadDayRecord("2026-10-01").sessions.isEmpty())
        assertEquals(listOf("2026-09-30"), storage.getAllRecordDates())
        assertFalse(sessions.currentRecord("2026-10-01").hasActiveSession)
    }

    @Test
    fun restartRestoresYesterdayAndPreventsDuplicateClockInAfterMidnight() {
        val storage = SerializedStorage(mutableMapOf("2026-12-31" to "${23 * hour},"))
        val restored = CheckinnSessions(storage)

        assertEquals("2026-12-31", restored.currentRecord("2027-01-01").date)
        val result = assertIs<CheckResult.AlreadyClockedIn>(restored.clockIn("2027-01-01", 25 * hour))
        assertEquals(23 * hour, result.existingTime)
        assertEquals(listOf("2026-12-31"), storage.getAllRecordDates())
        assertIs<CheckResult.ClockOutSuccess>(restored.clockOut(26 * hour))
        assertEquals(3 * hour, storage.loadDayRecord("2026-12-31").totalDurationMs)
    }

    @Test
    fun clockOutPreservesEarlierCompletedSessions() {
        val storage = SerializedStorage(mutableMapOf("2026-09-30" to "0,$hour;${23 * hour},"))
        val result = assertIs<CheckResult.ClockOutSuccess>(CheckinnSessions(storage).clockOut(25 * hour))

        assertEquals(2 * hour, result.sessionDuration)
        assertEquals(3 * hour, result.totalDuration)
        assertEquals(WorkSession(0, hour), storage.loadDayRecord("2026-09-30").sessions.first())
    }

    @Test
    fun anotherSessionCanStartOnTheNewDayAfterOvernightClockOut() {
        val storage = SerializedStorage()
        val sessions = CheckinnSessions(storage)
        sessions.clockIn("2026-09-30", 23 * hour)
        sessions.clockOut(25 * hour)
        sessions.clockIn("2026-10-01", 26 * hour)

        assertEquals("2026-10-01", sessions.currentRecord("2026-10-01").date)
        assertEquals(listOf(WorkSession(26 * hour, null)), storage.loadDayRecord("2026-10-01").sessions)
        assertEquals(listOf(WorkSession(23 * hour, 25 * hour)), storage.loadDayRecord("2026-09-30").sessions)
    }

    @Test
    fun clockOutWithoutAnActiveRecordDoesNotCreateOrModifyRecords() {
        val storage = SerializedStorage(mutableMapOf("2026-09-30" to "0,$hour"))
        assertEquals(CheckResult.NotClockedIn, CheckinnSessions(storage).clockOut(25 * hour))
        assertEquals("0,$hour", storage.values["2026-09-30"])
        assertEquals("2026-10-01", CheckinnSessions(storage).currentRecord("2026-10-01").date)
    }

    @Test
    fun legacyMultipleActiveDatesAreResolvedFromTheMostRecentRecordWithoutDroppingOlderData() {
        val storage = SerializedStorage(mutableMapOf("2026-09-29" to "1,", "2026-09-30" to "2,"))
        val sessions = CheckinnSessions(storage)

        assertEquals("2026-09-30", sessions.activeRecord()?.date)
        sessions.clockOut(3)
        assertEquals("2,3", storage.values["2026-09-30"])
        assertEquals("1,", storage.values["2026-09-29"])
    }

    @Test
    fun storageCodecKeepsTheExistingFormatAndUnfinishedSession() {
        val record = DayRecord("2026-09-30", listOf(WorkSession(100, 200), WorkSession(300, null)))
        assertEquals("100,200;300,", CheckinnRecordCodec.encode(record))
        assertEquals(record, CheckinnRecordCodec.decode(record.date, "100,200;300,"))
    }

    @Test
    fun storageCodecPreservesValidLegacyEntriesAndHandlesMissingValues() {
        assertEquals(DayRecord("2026-09-30"), CheckinnRecordCodec.decode("2026-09-30", null))
        assertEquals(DayRecord("2026-09-30"), CheckinnRecordCodec.decode("2026-09-30", ""))
        assertEquals(listOf(WorkSession(100, 200), WorkSession(300, null)),
            CheckinnRecordCodec.decode("2026-09-30", "100,200;invalid;300,;").sessions)
    }

    private class SerializedStorage(val values: MutableMap<String, String> = mutableMapOf()) : CheckinnStorageInterface {
        override fun saveDayRecord(record: DayRecord) { values[record.date] = CheckinnRecordCodec.encode(record) }
        override fun loadDayRecord(date: String) = CheckinnRecordCodec.decode(date, values[date])
        override fun getAllRecordDates() = values.keys.toList()
    }
}
