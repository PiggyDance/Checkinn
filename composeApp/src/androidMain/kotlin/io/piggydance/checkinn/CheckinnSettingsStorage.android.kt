package io.piggydance.checkinn

import android.content.Context
import android.content.SharedPreferences

class AndroidCheckinnSettingsStorage(context: Context) : CheckinnSettingsStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences("checkinn_settings", Context.MODE_PRIVATE)
    
    override fun saveSettings(settings: CheckinnSettings) {
        prefs.edit().apply {
            putInt("daily_goal_hours", settings.dailyGoalHours)
            putStringSet("work_days", settings.workDays.map { it.toString() }.toSet())
            apply()
        }
    }
    
    override fun loadSettings(): CheckinnSettings {
        val dailyGoalHours = prefs.getInt("daily_goal_hours", 8)
        val workDaysSet = prefs.getStringSet("work_days", setOf("0", "1", "2", "3", "4"))
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: setOf(0, 1, 2, 3, 4)
        
        return CheckinnSettings(
            dailyGoalHours = dailyGoalHours,
            workDays = workDaysSet
        )
    }
}
