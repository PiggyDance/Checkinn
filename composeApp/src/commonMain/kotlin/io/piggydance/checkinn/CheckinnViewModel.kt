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
    val isWriteMode: Boolean = false,          // 是否处于NFC写入模式
    val writeScene: NfcScene? = null,          // 写入的场景类型
    val writeSuccess: Boolean = false,         // 写入是否成功
)

enum class AnimationType {
    NONE, CLOCK_IN, CLOCK_OUT
}

class CheckinnViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinnUiState())
    val uiState: StateFlow<CheckinnUiState> = _uiState.asStateFlow()

    // 平台相关的存储层, 由 Android 侧注入
    var storage: CheckinnStorageInterface? = null

    fun initialize(storage: CheckinnStorageInterface) {
        this.storage = storage
        loadTodayRecord()
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
                    toastMessage = "你已经在上班中了哦！",
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
                toastMessage = "上班打卡成功！开始计时 ⏱",
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
                    toastMessage = "还没有上班打卡记录，请先上班打卡！",
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
                toastMessage = "下班打卡成功！今日累计工作 ${formatDuration(updatedRecord.totalDurationMs)}",
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
                toastMessage = if (success) "NFC 贴纸写入成功！" else "写入失败，请重试",
            )
        }
    }

    /** 刷新当前时间, 用于实时更新工作时长显示 */
    fun tick() {
        _uiState.update { it.copy(currentTimeMs = currentTimeMillis()) }
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return "${hours}小时${minutes}分${seconds}秒"
        }
    }
}

/** 平台无关的存储接口 */
interface CheckinnStorageInterface {
    fun saveDayRecord(record: DayRecord)
    fun loadDayRecord(date: String): DayRecord
    fun getAllRecordDates(): List<String>
}

expect fun todayDateString(): String
