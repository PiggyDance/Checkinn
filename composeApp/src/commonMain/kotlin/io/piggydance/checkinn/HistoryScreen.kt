package io.piggydance.checkinn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "打卡记录",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary,
            letterSpacing = (-0.3).sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 周/月切换 — 胶囊式分段控制
        ViewModeSelector(
            currentMode = uiState.historyViewMode,
            onModeChanged = { viewModel.switchHistoryMode(it) },
        )

        Spacer(modifier = Modifier.height(20.dp))

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

        // 底部留白，避免内容被悬浮导航栏遮挡
        Spacer(modifier = Modifier.height(130.dp))
    }
}

// ==================== 胶囊分段控制 ====================

@Composable
fun ViewModeSelector(currentMode: HistoryViewMode, onModeChanged: (HistoryViewMode) -> Unit) {
    // 胶囊底色
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentItem(
                label = "周视图",
                isSelected = currentMode == HistoryViewMode.WEEK,
                onClick = { onModeChanged(HistoryViewMode.WEEK) },
                modifier = Modifier.weight(1f),
            )
            SegmentItem(
                label = "月视图",
                isSelected = currentMode == HistoryViewMode.MONTH,
                onClick = { onModeChanged(HistoryViewMode.MONTH) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(200),
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primaryLight else AppColors.textMuted,
        animationSpec = tween(200),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(1.dp, AppColors.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
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

    Spacer(modifier = Modifier.height(16.dp))

    // 周统计概要 — 毛玻璃卡
    val totalWeekMs = uiState.weekRecords.sumOf { record ->
        record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    }
    val workDays = uiState.weekRecords.count { it.sessions.isNotEmpty() }

    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        glassAlpha = 0.08f,
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

    Spacer(modifier = Modifier.height(14.dp))

    // 每天行
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
        isSelected -> AppColors.primary.copy(alpha = 0.12f)
        isToday -> AppColors.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> AppColors.primary.copy(alpha = 0.20f)
        isToday -> AppColors.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    color = if (isToday) AppColors.primary else AppColors.textMuted,
                )
                Text(
                    text = "$dayNum",
                    fontSize = 17.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) AppColors.primaryLight else AppColors.textPrimary,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 时间区间条
            Column(modifier = Modifier.weight(1f)) {
                if (hasData) {
                    record.sessions.forEach { session ->
                        val clockIn = formatTime(session.clockInTime)
                        val clockOut = session.clockOutTime?.let { formatTime(it) } ?: "进行中"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp),
                        ) {
                            // 小色块
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 14.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(AppColors.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$clockIn - $clockOut",
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFamily,
                                color = AppColors.textSecondary,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "未打卡",
                        fontSize = 12.sp,
                        color = AppColors.textMuted.copy(alpha = 0.5f),
                    )
                }
            }

            // 总时长
            Text(
                text = if (hasData) CheckinnViewModel.formatDurationShort(totalMs) else "-",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFamily,
                color = if (hasData) AppColors.primaryLight else AppColors.textMuted.copy(alpha = 0.3f),
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

    Spacer(modifier = Modifier.height(16.dp))

    // 月统计概要 — 毛玻璃卡
    val totalMonthMs = uiState.monthRecords.sumOf { record ->
        record.sessions.filter { it.clockOutTime != null }.sumOf { it.durationMs }
    }
    val workDays = uiState.monthRecords.count { it.sessions.isNotEmpty() }

    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        glassAlpha = 0.08f,
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

    Spacer(modifier = Modifier.height(16.dp))

    // 星期标题行
    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        glassAlpha = 0.04f,
        borderAlpha = 0.06f,
        contentPadding = 12.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textMuted,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日历网格
        val firstDate = uiState.monthDates.first()
        val firstDayOfWeek = dayOfWeekIndex(firstDate)
        val totalCells = firstDayOfWeek + uiState.monthDates.size
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dateIndex = cellIndex - firstDayOfWeek

                    if (dateIndex < 0 || dateIndex >= uiState.monthDates.size) {
                        Box(modifier = Modifier.weight(1f).height(56.dp))
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
        totalMs < 4 * 3600_000L -> 0.15f
        totalMs < 8 * 3600_000L -> 0.25f
        else -> 0.40f
    }

    val bgColor = when {
        isSelected -> AppColors.primary.copy(alpha = 0.20f)
        hasData -> AppColors.primary.copy(alpha = intensity)
        else -> Color.Transparent
    }

    val borderMod = when {
        isToday -> Modifier.border(1.5.dp, AppColors.primary, RoundedCornerShape(10.dp))
        isSelected -> Modifier.border(1.dp, AppColors.primary.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
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
                color = when {
                    isToday -> AppColors.primaryLight
                    hasData -> AppColors.textPrimary
                    else -> AppColors.textMuted
                },
            )
            if (hasData) {
                Text(
                    text = CheckinnViewModel.formatHoursDecimal(totalMs) + "h",
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFamily,
                    color = AppColors.primaryLight,
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
        // 上一页按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable { onPrevious() },
            contentAlignment = Alignment.Center,
        ) {
            Text("◀", fontSize = 14.sp, color = AppColors.textSecondary)
        }

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            letterSpacing = 0.5.sp,
        )

        // 下一页按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable { onNext() },
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", fontSize = 14.sp, color = AppColors.textSecondary)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFamily,
            color = AppColors.primaryLight,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = AppColors.textMuted,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun DayDetailCard(record: DayRecord) {
    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
    ) {
        Text(
            text = "${formatShortDate(record.date)} 打卡详情",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        val totalMs = record.sessions
            .filter { it.clockOutTime != null }
            .sumOf { it.durationMs }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "总时长 ${CheckinnViewModel.formatDuration(totalMs)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = JetBrainsMonoFamily,
                color = AppColors.primaryLight,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        record.sessions.forEachIndexed { index, session ->
            val clockIn = formatTime(session.clockInTime)
            val clockOut = session.clockOutTime?.let { formatTime(it) } ?: "进行中"
            val dur = if (session.clockOutTime != null) {
                CheckinnViewModel.formatDuration(session.durationMs)
            } else "进行中"

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFamily,
                        color = AppColors.primary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$clockIn → $clockOut",
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMonoFamily,
                    color = AppColors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dur,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = JetBrainsMonoFamily,
                    color = AppColors.primaryLight,
                )
            }
            // 分隔线
            if (index < record.sessions.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .height(1.dp)
                        .background(AppColors.divider)
                )
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
