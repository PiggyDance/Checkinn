package io.piggydance.checkinn

/**
 * 多语言字符串资源接口
 * Android 平台使用 Android 资源系统实现
 * iOS 平台使用 Localizable.strings 实现
 */
interface StringResources {
    // Bottom Navigation
    fun navClock(): String
    fun navHistory(): String
    
    // Status
    fun statusWorking(): String
    fun statusIdle(): String
    fun todayTotal(): String
    fun todayGoal(): String
    fun remainHours(hours: String): String
    fun completed(): String
    
    // Sessions
    fun todaySessions(): String
    fun expandAll(): String
    fun collapse(): String
    fun inProgress(): String
    
    // Manual Check
    fun manualCheck(): String
    fun clockIn(): String
    fun clockOut(): String
    
    // NFC
    fun nfcSetup(): String
    fun nfcDescription(): String
    fun writeClockIn(): String
    fun writeClockOut(): String
    fun nfcWriteDialogTitle(): String
    fun nfcWriteInstruction(): String
    fun nfcWriteScene(scene: String): String
    fun cancel(): String
    
    // Toast Messages
    fun toastAlreadyWorking(): String
    fun toastClockInSuccess(): String
    fun toastNoClockIn(): String
    fun toastClockOutSuccess(duration: String): String
    fun toastNfcWriteSuccess(): String
    fun toastNfcWriteFailed(): String
    
    // Animation
    fun animClockInSuccess(): String
    fun animClockOutSuccess(): String
    
    // History
    fun viewWeek(): String
    fun viewMonth(): String
    fun weekFormat(week: String): String
    fun monthFormat(month: String): String
    fun dayDetails(day: String): String
    fun totalDuration(): String
    fun noRecord(): String
    fun workHours(hours: String): String
    
    // Day of Week - Short
    fun mondayShort(): String
    fun tuesdayShort(): String
    fun wednesdayShort(): String
    fun thursdayShort(): String
    fun fridayShort(): String
    fun saturdayShort(): String
    fun sundayShort(): String
    
    // Day of Week - Full
    fun monday(): String
    fun tuesday(): String
    fun wednesday(): String
    fun thursday(): String
    fun friday(): String
    fun saturday(): String
    fun sunday(): String
    
    // Settings
    fun settings(): String
    fun language(): String
    fun selectLanguage(): String
}

/**
 * 平台无关的字符串资源获取函数
 * 由各平台实现
 */
expect fun getStringResources(): StringResources
