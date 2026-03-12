package io.piggydance.checkinn

/**
 * iOS 平台的字符串资源实现
 * 由于 Kotlin Multiplatform 在 iOS 上使用较为复杂的本地化方案,
 * 这里使用硬编码的方式提供默认英文支持
 * 未来可以通过 NSLocalizedString 等机制实现完整的多语言支持
 */
class IosStringResources : StringResources {
    override fun navClock() = "Clock"
    override fun navHistory() = "History"
    
    override fun statusWorking() = "Working"
    override fun statusIdle() = "Off Work"
    override fun todayTotal() = "Today's Total"
    override fun todayGoal() = "Today's Goal 10h"
    override fun remainHours(hours: String) = "Remaining $hours"
    override fun completed() = "Completed ✓"
    
    override fun todaySessions() = "Today's Work Sessions"
    override fun expandAll() = "Expand All"
    override fun collapse() = "Collapse"
    override fun inProgress() = "In Progress"
    
    override fun manualCheck() = "Manual Check"
    override fun clockIn() = "Clock In"
    override fun clockOut() = "Clock Out"
    
    override fun nfcSetup() = "NFC Sticker Setup"
    override fun nfcDescription() = "Write check scene to blank NFC sticker"
    override fun writeClockIn() = "Write \"Clock In\" Card"
    override fun writeClockOut() = "Write \"Clock Out\" Card"
    override fun nfcWriteDialogTitle() = "Write NFC Sticker"
    override fun nfcWriteInstruction() = "Place blank NFC sticker near the back of your phone"
    override fun nfcWriteScene(scene: String) = "About to write: \"$scene\""
    override fun cancel() = "Cancel"
    
    override fun toastAlreadyWorking() = "You're already clocked in!"
    override fun toastClockInSuccess() = "Clock in successful! Timer started ⏱"
    override fun toastNoClockIn() = "No clock in record. Please clock in first!"
    override fun toastClockOutSuccess(duration: String) = "Clock out successful! Total work today: $duration"
    override fun toastNfcWriteSuccess() = "NFC sticker written successfully!"
    override fun toastNfcWriteFailed() = "Write failed, please try again"
    
    override fun animClockInSuccess() = "Clock In Successful!"
    override fun animClockOutSuccess() = "Clock Out Successful!"
    
    override fun viewWeek() = "Week View"
    override fun viewMonth() = "Month View"
    override fun weekFormat(week: String) = "Week $week"
    override fun monthFormat(month: String) = month
    override fun dayDetails(day: String) = "$day Records"
    override fun totalDuration() = "Total Duration"
    override fun noRecord() = "No Records"
    override fun workHours(hours: String) = "$hours hours"
    
    override fun mondayShort() = "Mon"
    override fun tuesdayShort() = "Tue"
    override fun wednesdayShort() = "Wed"
    override fun thursdayShort() = "Thu"
    override fun fridayShort() = "Fri"
    override fun saturdayShort() = "Sat"
    override fun sundayShort() = "Sun"
    
    override fun monday() = "Monday"
    override fun tuesday() = "Tuesday"
    override fun wednesday() = "Wednesday"
    override fun thursday() = "Thursday"
    override fun friday() = "Friday"
    override fun saturday() = "Saturday"
    override fun sunday() = "Sunday"
    
    override fun settings() = "Settings"
    override fun language() = "Language"
    override fun selectLanguage() = "Select Language"
}

actual fun getStringResources(): StringResources {
    return IosStringResources()
}
