package io.piggydance.checkinn

import androidx.compose.foundation.background
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkSettingsDialog(
    settings: CheckinnSettings,
    strings: StringResources,
    onDismiss: () -> Unit,
    onConfirm: (CheckinnSettings) -> Unit,
) {
    var dailyGoalHours by remember { mutableStateOf(settings.dailyGoalHours) }
    var selectedWorkDays by remember { mutableStateOf(settings.workDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgMid,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                strings.workSettings(),
                color = AppColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 每日目标时长
                Text(
                    text = strings.dailyGoalHours(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 时长显示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.primary.copy(alpha = 0.15f),
                                    AppColors.primaryLight.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(1.dp, AppColors.primary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.hoursFormat(dailyGoalHours),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFamily,
                        color = AppColors.primaryLight,
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 滑块
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
                
                // 0-12小时刻度提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0h", fontSize = 10.sp, color = AppColors.textMuted)
                    Text("12h", fontSize = 10.sp, color = AppColors.textMuted)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 工作日设置
                Text(
                    text = strings.workDaysSetting(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 星期选择器
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (0..6).forEach { dayIndex ->
                        val isSelected = selectedWorkDays.contains(dayIndex)
                        DayChip(
                            dayName = getDayShortName(dayIndex, strings),
                            isSelected = isSelected,
                            onClick = {
                                selectedWorkDays = if (isSelected) {
                                    selectedWorkDays - dayIndex
                                } else {
                                    selectedWorkDays + dayIndex
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CheckinnSettings(
                            dailyGoalHours = dailyGoalHours,
                            workDays = selectedWorkDays
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(strings.confirm())
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = AppColors.textSecondary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(strings.cancel())
            }
        },
    )
}

@Composable
private fun DayChip(
    dayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AppColors.primary, AppColors.primaryLight)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    AppColors.primary.copy(alpha = 0.3f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = dayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
