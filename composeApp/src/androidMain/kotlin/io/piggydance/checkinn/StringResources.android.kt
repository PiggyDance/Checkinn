package io.piggydance.checkinn

import android.content.Context

private lateinit var appContext: Context

/**
 * 初始化应用上下文
 */
fun initializeContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * Android 平台的字符串资源实现
 */
class AndroidStringResources(private val context: Context) : StringResources {
    override fun navClock() = context.getString(R.string.nav_clock)
    override fun navHistory() = context.getString(R.string.nav_history)
    
    override fun statusWorking() = context.getString(R.string.status_working)
    override fun statusIdle() = context.getString(R.string.status_idle)
    override fun todayTotal() = context.getString(R.string.today_total)
    override fun todayGoal() = context.getString(R.string.today_goal)
    override fun remainHours(hours: String) = context.getString(R.string.remain_hours, hours)
    override fun completed() = context.getString(R.string.completed)
    
    override fun todaySessions() = context.getString(R.string.today_sessions)
    override fun expandAll() = context.getString(R.string.expand_all)
    override fun collapse() = context.getString(R.string.collapse)
    override fun inProgress() = context.getString(R.string.in_progress)
    
    override fun manualCheck() = context.getString(R.string.manual_check)
    override fun clockIn() = context.getString(R.string.clock_in)
    override fun clockOut() = context.getString(R.string.clock_out)
    
    override fun nfcSetup() = context.getString(R.string.nfc_setup)
    override fun nfcDescription() = context.getString(R.string.nfc_description)
    override fun writeClockIn() = context.getString(R.string.write_clock_in)
    override fun writeClockOut() = context.getString(R.string.write_clock_out)
    override fun nfcWriteDialogTitle() = context.getString(R.string.nfc_write_dialog_title)
    override fun nfcWriteInstruction() = context.getString(R.string.nfc_write_instruction)
    override fun nfcWriteScene(scene: String) = context.getString(R.string.nfc_write_scene, scene)
    override fun cancel() = context.getString(R.string.cancel)
    
    override fun toastAlreadyWorking() = context.getString(R.string.toast_already_working)
    override fun toastClockInSuccess() = context.getString(R.string.toast_clock_in_success)
    override fun toastNoClockIn() = context.getString(R.string.toast_no_clock_in)
    override fun toastClockOutSuccess(duration: String) = context.getString(R.string.toast_clock_out_success, duration)
    override fun toastNfcWriteSuccess() = context.getString(R.string.toast_nfc_write_success)
    override fun toastNfcWriteFailed() = context.getString(R.string.toast_nfc_write_failed)
    
    override fun animClockInSuccess() = context.getString(R.string.anim_clock_in_success)
    override fun animClockOutSuccess() = context.getString(R.string.anim_clock_out_success)
    
    override fun viewWeek() = context.getString(R.string.view_week)
    override fun viewMonth() = context.getString(R.string.view_month)
    override fun weekFormat(week: String) = context.getString(R.string.week_format, week)
    override fun monthFormat(month: String) = context.getString(R.string.month_format, month)
    override fun dayDetails(day: String) = context.getString(R.string.day_details, day)
    override fun totalDuration() = context.getString(R.string.total_duration)
    override fun noRecord() = context.getString(R.string.no_record)
    override fun workHours(hours: String) = context.getString(R.string.work_hours, hours)
    
    override fun mondayShort() = context.getString(R.string.monday_short)
    override fun tuesdayShort() = context.getString(R.string.tuesday_short)
    override fun wednesdayShort() = context.getString(R.string.wednesday_short)
    override fun thursdayShort() = context.getString(R.string.thursday_short)
    override fun fridayShort() = context.getString(R.string.friday_short)
    override fun saturdayShort() = context.getString(R.string.saturday_short)
    override fun sundayShort() = context.getString(R.string.sunday_short)
    
    override fun monday() = context.getString(R.string.monday)
    override fun tuesday() = context.getString(R.string.tuesday)
    override fun wednesday() = context.getString(R.string.wednesday)
    override fun thursday() = context.getString(R.string.thursday)
    override fun friday() = context.getString(R.string.friday)
    override fun saturday() = context.getString(R.string.saturday)
    override fun sunday() = context.getString(R.string.sunday)
    
    override fun settings() = context.getString(R.string.settings)
    override fun language() = context.getString(R.string.language)
    override fun selectLanguage() = context.getString(R.string.select_language)
}

actual fun getStringResources(): StringResources {
    return AndroidStringResources(appContext)
}
