package io.piggydance.checkinn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Keep explicit saving and protect a changed draft when navigating back. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkSettingsScreen(
    settings: CheckinnSettings,
    strings: StringResources,
    onBack: () -> Unit,
    onConfirm: (CheckinnSettings) -> Unit,
    backEnabled: Boolean = true,
    nfcContent: @Composable () -> Unit,
) {
    var dailyGoalHours by remember(settings) { mutableStateOf(settings.dailyGoalHours) }
    var selectedWorkDays by remember(settings) { mutableStateOf(settings.workDays) }
    var showExitPrompt by remember { mutableStateOf(false) }
    val saveDraft = { onConfirm(CheckinnSettings(dailyGoalHours = dailyGoalHours, workDays = selectedWorkDays)) }
    val requestBack = {
        if (dailyGoalHours != settings.dailyGoalHours || selectedWorkDays != settings.workDays) {
            showExitPrompt = true
        } else {
            onBack()
        }
    }
    SettingsBackHandler(enabled = backEnabled, onBack = requestBack)
    if (showExitPrompt) {
        AlertDialog(
            onDismissRequest = { showExitPrompt = false },
            containerColor = AppColors.bgMid,
            shape = RoundedCornerShape(24.dp),
            title = { Text(strings.unsavedChanges(), color = AppColors.textPrimary) },
            confirmButton = {
                TextButton(onClick = saveDraft) { Text(strings.save()) }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text(strings.discard(), color = AppColors.textSecondary) }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = requestBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.back(),
                    tint = AppColors.textSecondary,
                )
            }
            Text(
                strings.settings(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GlassCardColumn(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp, cornerRadius = 20.dp) {
                Text(strings.workSettings(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
                Spacer(Modifier.height(20.dp))
                Text(strings.dailyGoalHours(), fontSize = 14.sp, color = AppColors.textSecondary)
                Spacer(Modifier.height(12.dp))
                Text(
                    strings.hoursFormat(dailyGoalHours),
                    fontSize = 30.sp,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primaryLight,
                )
                Slider(
                    value = dailyGoalHours.toFloat(),
                    onValueChange = { dailyGoalHours = it.roundToInt() },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.primaryLight,
                        activeTrackColor = AppColors.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(strings.workDaysSetting(), fontSize = 14.sp, color = AppColors.textSecondary)
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4,
                ) {
                    (0..6).forEach { dayIndex ->
                        val selected = dayIndex in selectedWorkDays
                        DayChip(getDayShortName(dayIndex, strings), selected) {
                            selectedWorkDays = if (selected) selectedWorkDays - dayIndex else selectedWorkDays + dayIndex
                        }
                    }
                }
            }
            nfcContent()
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = saveDraft,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary, contentColor = Color.White),
        ) {
            Text(strings.save(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DayChip(dayName: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .background(
                Brush.linearGradient(
                    if (isSelected) listOf(AppColors.primary, AppColors.primaryLight)
                    else listOf(AppColors.glassBg, Color.White.copy(alpha = 0.04f))
                )
            )
            .border(1.dp, if (isSelected) AppColors.primary else AppColors.glassBorder, CircleShape)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center,
    ) {
        // Keep the weekday visible even when selected.
        Text(
            dayName,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else AppColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
