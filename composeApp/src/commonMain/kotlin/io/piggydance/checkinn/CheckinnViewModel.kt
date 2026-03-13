package io.piggydance.checkinn

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI 状态 */
data class CheckinnUiState(
    val todayRecord: DayRecord = DayRecord(date = ""),
    val currentTimeMs: Long = 0L,
    val showAnimation: Boolean = false,
    val animationType: AnimationType = AnimationType.NONE,
    val toastMessage: String? = null,
    val isWriteMode: Boolean = false,
    val writeScene: NfcScene? = null,
    val writeSuccess: Boolean = false,
    // 设置
    val settings: CheckinnSettings = CheckinnSettings(),
    val showSettingsDialog: Boolean = false,
    // 历史视图
    val historyViewMode: HistoryViewMode = HistoryViewMode.WEEK,
    val weekAnchorDate: String = "",       // 当前周视图的锚点日期
    val monthAnchorDate: String = "",      // 当前月视图的锚点日期
    val weekRecords: List<DayRecord> = emptyList(),  // 当前周的记录
    val monthRecords: List<DayRecord> = emptyList(), // 当前月的记录
    val weekDates: List<String> = emptyList(),
    val monthDates: List<String> = emptyList(),
    val selectedDayRecord: DayRecord? = null, // 点击某天后查看详情
)

enum class AnimationType {
    NONE, CLOCK_IN, CLOCK_OUT
}

enum class HistoryViewMode {
    WEEK, MONTH
}

class CheckinnViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinnUiState())
    val uiState: StateFlow<CheckinnUiState> = _uiState.asStateFlow()

    // 平台相关的存储层, 由 Android 侧注入
    var storage: CheckinnStorageInterface? = null
    var settingsStorage: CheckinnSettingsStorage? = null
    
    // 字符串资源 - 公开访问，供 UI 层使用
    val strings: StringResources by lazy { getStringResources() }

    fun initialize(storage: CheckinnStorageInterface, settingsStorage: CheckinnSettingsStorage) {
        this.storage = storage
        this.settingsStorage = settingsStorage
        loadSettings()
        loadTodayRecord()
    }
    
    private fun loadSettings() {
        val settings = settingsStorage?.loadSettings() ?: CheckinnSettings()
        _uiState.update { it.copy(settings = settings) }
    }

    private fun loadTodayRecord() {
        val today = todayDateString()
        val record = storage?.loadDayRecord(today) ?: DayRecord(date = today)
        _uiState.update { it.copy(todayRecord = record, currentTimeMs = currentTimeMillis()) }
    }

    /** 处理 NFC 扫描事件 */
    fun onNfcScanned(scene: NfcScene) {
        when (scene) {
            NfcScene.CLOCK_IN -> handleClockIn()
            NfcScene.CLOCK_OUT -> handleClockOut()
        }
    }

    private fun handleClockIn() {
        val now = currentTimeMillis()
        val today = todayDateString()
        val record = storage?.loadDayRecord(today) ?: DayRecord(date = today)

        if (record.hasActiveSession) {
            // 已经打过上班卡且还没下班
            _uiState.update {
                it.copy(
                    toastMessage = strings.toastAlreadyWorking(),
                    todayRecord = record,
                )
            }
            return
        }

        // 创建新的工作段
        val newSession = WorkSession(clockInTime = now, clockOutTime = null)
        val updatedRecord = record.copy(sessions = record.sessions + newSession)
        storage?.saveDayRecord(updatedRecord)

        _uiState.update {
            it.copy(
                todayRecord = updatedRecord,
                showAnimation = true,
                animationType = AnimationType.CLOCK_IN,
                currentTimeMs = now,
                toastMessage = strings.toastClockInSuccess(),
            )
        }
    }

    private fun handleClockOut() {
        val now = currentTimeMillis()
        val today = todayDateString()
        val record = storage?.loadDayRecord(today) ?: DayRecord(date = today)

        if (!record.hasActiveSession) {
            _uiState.update {
                it.copy(
                    toastMessage = strings.toastNoClockIn(),
                    todayRecord = record,
                )
            }
            return
        }

        // 结束当前活跃的工作段
        val updatedSessions = record.sessions.map { session ->
            if (session.clockOutTime == null) {
                session.copy(clockOutTime = now)
            } else {
                session
            }
        }
        val updatedRecord = record.copy(sessions = updatedSessions)
        storage?.saveDayRecord(updatedRecord)

        _uiState.update {
            it.copy(
                todayRecord = updatedRecord,
                showAnimation = true,
                animationType = AnimationType.CLOCK_OUT,
                currentTimeMs = now,
                toastMessage = strings.toastClockOutSuccess(formatDurationChinese(updatedRecord.totalDurationMs)),
            )
        }
    }

    fun dismissAnimation() {
        _uiState.update { it.copy(showAnimation = false, animationType = AnimationType.NONE) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    /** 进入NFC写入模式 */
    fun enterWriteMode(scene: NfcScene) {
        _uiState.update { it.copy(isWriteMode = true, writeScene = scene, writeSuccess = false) }
    }

    /** 退出NFC写入模式 */
    fun exitWriteMode() {
        _uiState.update { it.copy(isWriteMode = false, writeScene = null, writeSuccess = false) }
    }

    /** NFC 写入完成 */
    fun onWriteComplete(success: Boolean) {
        _uiState.update {
            it.copy(
                writeSuccess = success,
                isWriteMode = !success, // 成功就退出写入模式
                toastMessage = if (success) strings.toastNfcWriteSuccess() else strings.toastNfcWriteFailed(),
            )
        }
    }

    /** 刷新当前时间, 用于实时更新工作时长显示 */
    fun tick() {
        _uiState.update { it.copy(currentTimeMs = currentTimeMillis()) }
    }

    // ========== 历史视图 ==========

    /** 初始化历史视图, 默认显示当前周 */
    fun initHistory() {
        val today = todayDateString()
        _uiState.update {
            it.copy(weekAnchorDate = today, monthAnchorDate = today)
        }
        loadWeekData(today)
        loadMonthData(today)
    }

    fun switchHistoryMode(mode: HistoryViewMode) {
        _uiState.update { it.copy(historyViewMode = mode, selectedDayRecord = null) }
    }

    fun previousWeek() {
        val newAnchor = offsetWeek(_uiState.value.weekAnchorDate, -1)
        _uiState.update { it.copy(weekAnchorDate = newAnchor, selectedDayRecord = null) }
        loadWeekData(newAnchor)
    }

    fun nextWeek() {
        val newAnchor = offsetWeek(_uiState.value.weekAnchorDate, 1)
        _uiState.update { it.copy(weekAnchorDate = newAnchor, selectedDayRecord = null) }
        loadWeekData(newAnchor)
    }

    fun previousMonth() {
        val newAnchor = offsetMonth(_uiState.value.monthAnchorDate, -1)
        _uiState.update { it.copy(monthAnchorDate = newAnchor, selectedDayRecord = null) }
        loadMonthData(newAnchor)
    }

    fun nextMonth() {
        val newAnchor = offsetMonth(_uiState.value.monthAnchorDate, 1)
        _uiState.update { it.copy(monthAnchorDate = newAnchor, selectedDayRecord = null) }
        loadMonthData(newAnchor)
    }

    fun selectDay(date: String) {
        val record = storage?.loadDayRecord(date) ?: DayRecord(date = date)
        _uiState.update { it.copy(selectedDayRecord = record) }
    }

    fun clearSelectedDay() {
        _uiState.update { it.copy(selectedDayRecord = null) }
    }
    
    // ========== 设置相关 ==========
    
    fun showSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }
    
    fun hideSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }
    
    fun updateSettings(settings: CheckinnSettings) {
        settingsStorage?.saveSettings(settings)
        _uiState.update { it.copy(settings = settings, showSettingsDialog = false) }
    }

    private fun loadWeekData(anchorDate: String) {
        val dates = getWeekDates(anchorDate)
        val records = storage?.loadDayRecords(dates) ?: dates.map { DayRecord(date = it) }
        _uiState.update { it.copy(weekDates = dates, weekRecords = records) }
    }

    private fun loadMonthData(anchorDate: String) {
        val dates = getMonthDates(anchorDate)
        val records = storage?.loadDayRecords(dates) ?: dates.map { DayRecord(date = it) }
        _uiState.update { it.copy(monthDates = dates, monthRecords = records) }
    }

    companion object {
        /** 固定宽度格式: "00:05:03" — 避免数字位数变化导致宽度跳变 */
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return "${pad2(h)}:${pad2(m)}:${pad2(s)}"
        }

        /** 固定宽度短格式: "08h05m" 或 "05m" */
        fun formatDurationShort(ms: Long): String {
            val totalMinutes = ms / 60000
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (h > 0) "${pad2(h)}h${pad2(m)}m" else "${pad2(m)}m"
        }

        fun formatHoursDecimal(ms: Long): String {
            val totalMinutes = ms / 60000
            val hours = totalMinutes / 60
            val tenthHour = (totalMinutes % 60) * 10 / 60
            return "${hours}.${tenthHour}"
        }

        /** Toast 消息用中文格式: "1小时5分" */
        fun formatDurationChinese(ms: Long): String {
            val totalSeconds = ms / 1000
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            return if (h > 0) "${h}小时${m}分" else "${m}分钟"
        }

        /** 两位补零 */
        private fun pad2(n: Long): String = if (n < 10) "0$n" else "$n"
    }
}

/** 平台无关的存储接口 */
interface CheckinnStorageInterface {
    fun saveDayRecord(record: DayRecord)
    fun loadDayRecord(date: String): DayRecord
    fun getAllRecordDates(): List<String>

    /** 批量加载多个日期的记录 */
    fun loadDayRecords(dates: List<String>): List<DayRecord> {
        return dates.map { loadDayRecord(it) }
    }
}

expect fun todayDateString(): String
