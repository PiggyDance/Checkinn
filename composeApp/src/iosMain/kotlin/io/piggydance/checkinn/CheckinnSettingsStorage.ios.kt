package io.piggydance.checkinn

import platform.Foundation.NSUserDefaults

class IosCheckinnSettingsStorage : CheckinnSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    
    override fun saveSettings(settings: CheckinnSettings) {
        defaults.setInteger(settings.dailyGoalHours.toLong(), "daily_goal_hours")
        defaults.setObject(settings.workDays.map { it.toLong() }, "work_days")
        defaults.synchronize()
    }
    
    override fun loadSettings(): CheckinnSettings {
        val dailyGoalHours = defaults.integerForKey("daily_goal_hours").toInt()
            .takeIf { it in 0..12 } ?: 8
        
        @Suppress("UNCHECKED_CAST")
        val workDaysArray = defaults.arrayForKey("work_days") as? List<Long>
        val workDaysSet = workDaysArray?.map { it.toInt() }?.toSet() 
            ?: setOf(0, 1, 2, 3, 4)
        
        return CheckinnSettings(
            dailyGoalHours = dailyGoalHours,
            workDays = workDaysSet
        )
    }
}
