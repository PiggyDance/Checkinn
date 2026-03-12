package io.piggydance.checkinn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(viewModel: CheckinnViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initHistory()
    }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "打卡记录",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 周/月切换
        ViewModeSelector(
            currentMode = uiState.historyViewMode,
            onModeChanged = { viewModel.switchHistoryMode(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.historyViewMode) {
            HistoryViewMode.WEEK -> WeekView(uiState = uiState, viewModel = viewModel)
            HistoryViewMode.MONTH -> MonthView(uiState = uiState, viewModel = viewModel)
        }

        // 选中日的详情
        uiState.selectedDayRecord?.let { record ->
            if (record.sessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DayDetailCard(record = record)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ViewModeSelector(currentMode: HistoryViewMode, onModeChanged: (HistoryViewMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        FilterChip(
            selected = currentMode == HistoryViewMode.WEEK,
            onClick = { onModeChanged(HistoryViewMode.WEEK) },
            label = { Text("周视图") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4CAF50),
                selectedLabelColor = Color.White,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        FilterChip(
            selected = currentMode == HistoryViewMode.MONTH,
            onClick = { onModeChanged(HistoryViewMode.MONTH) },
            label = { Text("月视图") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4CAF50),
                selectedLabelColor = Color.White,
            ),
        )
    }
}

// ==================== 周视图 ====================

@Composable
fun WeekView(uiState: CheckinnUiState, viewModel: CheckinnViewModel) {
    if (uiState.weekDates.isEmpty()) return

    val weekStart = uiState.weekDates.first()
    val weekEnd = uiState.weekDates.last()
    val today = todayDateString()

    // 导航头
    NavigationHeader(
        title = formatWeekHeader(weekStart, weekEnd),
        onPrevious = { viewModel.previousWeek() },
        onNext = { viewModel.nextWeek() },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 周统计概要
    val totalWeekMs = uiState.weekRecords.sumOf { record ->
        record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    }
    val workDays = uiState.weekRecords.count { it.sessions.isNotEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(label = "本周累计", value = CheckinnViewModel.formatDurationShort(totalWeekMs))
            StatItem(label = "出勤天数", value = "${workDays}天")
            StatItem(
                label = "日均",
                value = if (workDays > 0)
                    CheckinnViewModel.formatDurationShort(totalWeekMs / workDays)
                else "0m"
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 每天的时间线条形图 + 时间区间详情
    uiState.weekDates.forEachIndexed { index, date ->
        val record = uiState.weekRecords.getOrNull(index) ?: DayRecord(date = date)
        val isToday = date == today
        val isSelected = uiState.selectedDayRecord?.date == date

        WeekDayRow(
            date = date,
            record = record,
            isToday = isToday,
            isSelected = isSelected,
            onClick = { viewModel.selectDay(date) },
        )
    }
}

@Composable
fun WeekDayRow(
    date: String,
    record: DayRecord,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dayLabel = dayOfWeekShort(date)
    val dayNum = dayOfMonth(date)
    val totalMs = record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    val hasData = record.sessions.isNotEmpty()

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> Color(0xFF4CAF50).copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 日期标签
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp),
            ) {
                Text(
                    text = "周$dayLabel",
                    fontSize = 11.sp,
                    color = if (isToday) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$dayNum",
                    fontSize = 16.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 时间区间条
            Column(modifier = Modifier.weight(1f)) {
                if (hasData) {
                    // 显示每段的时间区间
                    record.sessions.forEach { session ->
                        val clockIn = formatTime(session.clockInTime)
                        val clockOut = session.clockOutTime?.let { formatTime(it) } ?: "进行中"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 小色块
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$clockIn - $clockOut",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "未打卡",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }

            // 总时长
            Text(
                text = if (hasData) CheckinnViewModel.formatDurationShort(totalMs) else "-",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasData) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.width(56.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

// ==================== 月视图 ====================

@Composable
fun MonthView(uiState: CheckinnUiState, viewModel: CheckinnViewModel) {
    if (uiState.monthDates.isEmpty()) return

    val today = todayDateString()

    // 导航头
    NavigationHeader(
        title = formatYearMonth(uiState.monthAnchorDate),
        onPrevious = { viewModel.previousMonth() },
        onNext = { viewModel.nextMonth() },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 月统计概要
    val totalMonthMs = uiState.monthRecords.sumOf { record ->
        record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    }
    val workDays = uiState.monthRecords.count { it.sessions.isNotEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(label = "本月累计", value = CheckinnViewModel.formatDurationShort(totalMonthMs))
            StatItem(label = "出勤天数", value = "${workDays}天")
            StatItem(
                label = "日均",
                value = if (workDays > 0)
                    CheckinnViewModel.formatDurationShort(totalMonthMs / workDays)
                else "0m"
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 星期标题行
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
            Text(
                text = d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 日历网格
    // 先算第一天是星期几，补齐前面的空白
    val firstDate = uiState.monthDates.first()
    val firstDayOfWeek = dayOfWeekIndex(firstDate) // 0=周一

    // 所有格子 = 前置空白 + 实际天数
    val totalCells = firstDayOfWeek + uiState.monthDates.size
    val rows = (totalCells + 6) / 7

    for (row in 0 until rows) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val dateIndex = cellIndex - firstDayOfWeek

                if (dateIndex < 0 || dateIndex >= uiState.monthDates.size) {
                    // 空白格
                    Box(modifier = Modifier.weight(1f).height(60.dp))
                } else {
                    val date = uiState.monthDates[dateIndex]
                    val record = uiState.monthRecords.getOrNull(dateIndex)
                        ?: DayRecord(date = date)
                    val isToday = date == today
                    val isSelected = uiState.selectedDayRecord?.date == date

                    MonthDayCell(
                        date = date,
                        record = record,
                        isToday = isToday,
                        isSelected = isSelected,
                        onClick = { viewModel.selectDay(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun MonthDayCell(
    date: String,
    record: DayRecord,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayNum = dayOfMonth(date)
    val totalMs = record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    val hasData = record.sessions.isNotEmpty()

    // 根据时长调整颜色深浅
    val intensity = when {
        totalMs <= 0 -> 0f
        totalMs < 4 * 3600_000L -> 0.3f   // < 4小时
        totalMs < 8 * 3600_000L -> 0.6f   // < 8小时
        else -> 1.0f                        // >= 8小时
    }

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasData -> Color(0xFF4CAF50).copy(alpha = intensity * 0.3f)
        else -> Color.Transparent
    }

    val borderMod = if (isToday) {
        Modifier.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(60.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(borderMod)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$dayNum",
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color(0xFF4CAF50)
                else MaterialTheme.colorScheme.onSurface,
            )
            if (hasData) {
                Text(
                    text = CheckinnViewModel.formatHoursDecimal(totalMs) + "h",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ==================== 公共组件 ====================

@Composable
fun NavigationHeader(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPrevious() },
            contentAlignment = Alignment.Center,
        ) {
            Text("◀", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onNext() },
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DayDetailCard(record: DayRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${formatShortDate(record.date)} 打卡详情",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val totalMs = record.sessions
                .filter { it.clockOutTime != null }
                .sumOf { it.durationMs }
            Text(
                text = "总时长: ${CheckinnViewModel.formatDuration(totalMs)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4CAF50),
            )

            Spacer(modifier = Modifier.height(8.dp))

            record.sessions.forEachIndexed { index, session ->
                val clockIn = formatTime(session.clockInTime)
                val clockOut = session.clockOutTime?.let { formatTime(it) } ?: "进行中"
                val dur = if (session.clockOutTime != null) {
                    CheckinnViewModel.formatDuration(session.durationMs)
                } else "进行中"

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#${index + 1}  $clockIn → $clockOut",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = dur,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** 获取某天是星期几的索引, 0=周一 ... 6=周日 */
private fun dayOfWeekIndex(dateString: String): Int {
    val dayName = dayOfWeekShort(dateString)
    return when (dayName) {
        "一" -> 0; "二" -> 1; "三" -> 2; "四" -> 3
        "五" -> 4; "六" -> 5; "日" -> 6
        else -> 0
    }
}
