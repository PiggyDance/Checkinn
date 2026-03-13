package io.piggydance.checkinn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.InsertChartOutlined
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkinn.composeapp.generated.resources.Res
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import kotlinx.coroutines.delay

enum class AppTab { HOME, HISTORY }

@Composable
fun App(viewModel: CheckinnViewModel = CheckinnViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    val hazeState = remember { HazeState() }
    val strings = remember { getStringResources() }

    // 每秒刷新一次当前时间
    LaunchedEffect(uiState.todayRecord.hasActiveSession) {
        while (uiState.todayRecord.hasActiveSession) {
            delay(1000)
            viewModel.tick()
        }
    }

    // Toast 消息
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    CheckinnTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // 全屏渐变背景
            AppBackground {
                // 主内容区域（全屏，应用模糊效果）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState)
                ) {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(viewModel = viewModel, uiState = uiState)
                        AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    }
                    
                    // 底部导航栏也在这里，这样设置对话框可以模糊所有内容
                    GlassBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        hazeState = hazeState,
                        strings = strings,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                // Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 20.dp, end = 20.dp),
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = AppColors.textPrimary,
                    )
                }
            }

            // Lottie 动画覆盖层
            AnimatedVisibility(
                visible = uiState.showAnimation,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier.fillMaxSize(),
            ) {
                LottieOverlay(
                    animationType = uiState.animationType,
                    strings = strings,
                    onDismiss = { viewModel.dismissAnimation() },
                )
            }

            // NFC 写入提示对话框
            if (uiState.isWriteMode) {
                NfcWriteDialog(
                    scene = uiState.writeScene,
                    strings = strings,
                    onDismiss = { viewModel.exitWriteMode() },
                )
            }
            
        }
        
        // 工作设置对话框 - 覆盖层（在主内容区域外层，这样可以模糊背后内容）
        AnimatedVisibility(
            visible = uiState.showSettingsDialog,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            WorkSettingsDialog(
                settings = uiState.settings,
                strings = strings,
                hazeState = hazeState,
                onDismiss = { viewModel.hideSettingsDialog() },
                onConfirm = { viewModel.updateSettings(it) }
            )
        }
    }
}

// ==================== 毛玻璃底部导航 ====================

@Composable
fun GlassBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    hazeState: HazeState,
    strings: StringResources,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(state = hazeState) {
                backgroundColor = AppColors.bgTop.copy(alpha = 1f)
                blurRadius = 24.dp
                noiseFactor = 0.2f
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.bgTop.copy(alpha = 0.6f),
                        AppColors.bgBottom.copy(alpha = 0.7f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                    )
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* 消费点击事件，防止穿透到下层 */ }
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavItem(
                label = strings.navClock(),
                icon = Icons.Rounded.Fingerprint,
                isSelected = currentTab == AppTab.HOME,
                onClick = { onTabSelected(AppTab.HOME) },
            )
            NavItem(
                label = strings.navHistory(),
                icon = Icons.Rounded.InsertChartOutlined,
                isSelected = currentTab == AppTab.HISTORY,
                onClick = { onTabSelected(AppTab.HISTORY) },
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else Color.Transparent,
        animationSpec = tween(250),
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else AppColors.textMuted,
        animationSpec = tween(250),
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else AppColors.textMuted,
        animationSpec = tween(250),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 选中指示条
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(indicatorColor),
        )
    }
}

// ==================== 首页 ====================

@Composable
fun HomeScreen(viewModel: CheckinnViewModel, uiState: CheckinnUiState) {
    val strings = remember { getStringResources() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // App 标题 - 左对齐，使用 Orbitron 字体
        Text(
            text = "Checkinn",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OrbitronFamily,
            color = AppColors.textPrimary,
            letterSpacing = 0.sp,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(modifier = Modifier.height(32.dp))

        StatusCard(
            uiState = uiState,
            strings = strings,
            onGoalClick = { viewModel.showSettingsDialog() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SessionsCard(uiState = uiState, strings = strings)

        Spacer(modifier = Modifier.height(16.dp))

        ManualCheckButtons(viewModel = viewModel, strings = strings)

        Spacer(modifier = Modifier.height(16.dp))

        NfcWriteSection(viewModel = viewModel, uiState = uiState, strings = strings)

        // 底部留白，避免内容被悬浮导航栏遮挡
        Spacer(modifier = Modifier.height(130.dp))
    }
}

// ==================== 状态卡 ====================

@Composable
fun StatusCard(uiState: CheckinnUiState, strings: StringResources, onGoalClick: () -> Unit) {
    val isWorking = uiState.todayRecord.hasActiveSession

    // 工作中 = 绿色渐变毛玻璃, 未工作 = 普通毛玻璃
    val bgBrush = if (isWorking) {
        Brush.linearGradient(
            colors = listOf(
                AppColors.primary.copy(alpha = 0.20f),
                AppColors.primaryDim.copy(alpha = 0.10f),
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 400f),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.03f),
            ),
        )
    }

    val borderColor = if (isWorking) {
        AppColors.primary.copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val statusText = if (isWorking) strings.statusWorking() else strings.statusIdle()
    val statusDot = if (isWorking) AppColors.primary else AppColors.idle

    val totalDuration = if (isWorking) {
        val completedMs = uiState.todayRecord.sessions
            .filter { it.clockOutTime != null }
            .sumOf { it.durationMs }
        val activeMs = uiState.todayRecord.activeSession?.let {
            uiState.currentTimeMs - it.clockInTime
        } ?: 0L
        completedMs + activeMs
    } else {
        uiState.todayRecord.totalDurationMs
    }

    // 工作目标（毫秒）
    val targetMs = uiState.settings.dailyGoalHours * 60L * 60 * 1000
    val remainingMs = targetMs - totalDuration
    val progress = if (targetMs > 0) {
        (totalDuration.toFloat() / targetMs).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：状态指示灯 + 状态文字
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态指示灯
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusDot)
                            .then(
                                if (isWorking) Modifier.border(3.dp, statusDot.copy(alpha = 0.3f), CircleShape)
                                else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary,
                        letterSpacing = 0.5.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 今日累计
                Text(
                    text = strings.todayTotal(),
                    fontSize = 11.sp,
                    color = AppColors.textMuted,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CheckinnViewModel.formatDuration(totalDuration),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFamily,
                    color = if (isWorking) AppColors.primaryLight else AppColors.textPrimary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 右侧：目标进度（始终显示，支持点击）
            if (targetMs > 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onGoalClick() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = strings.todayGoalHours(uiState.settings.dailyGoalHours),
                        fontSize = 11.sp,
                        color = AppColors.textMuted,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // 进度条
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(AppColors.primary, AppColors.primaryLight)
                                    )
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    if (remainingMs > 0) {
                        Text(
                            text = strings.remainHours(CheckinnViewModel.formatDurationShort(remainingMs)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = JetBrainsMonoFamily,
                            color = AppColors.accent,
                        )
                    } else {
                        Text(
                            text = strings.completed(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.primary,
                        )
                    }
                }
            }
        }
    }
}

// ==================== 工作时段卡 ====================

@Composable
fun SessionsCard(uiState: CheckinnUiState, strings: StringResources) {
    if (uiState.todayRecord.sessions.isEmpty()) return

    // 折叠/展开状态
    var isExpanded by remember { mutableStateOf(false) }
    val sessions = uiState.todayRecord.sessions.asReversed()
    val hasMore = sessions.size > 4
    val displayedSessions = if (hasMore && !isExpanded) sessions.take(4) else sessions

    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
    ) {
        // 标题栏 - 如果超过4条，添加展开/收起按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.todaySessions(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
            )
            
            if (hasMore) {
                // 展开/收起按钮
                val iconRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(300)
                )
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.primary.copy(alpha = 0.12f))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isExpanded) strings.collapse() else strings.expandAll(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) strings.collapse() else strings.expandAll(),
                        tint = AppColors.primary,
                        modifier = Modifier.size(16.dp).rotate(iconRotation),
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        displayedSessions.forEachIndexed { index, session ->
            val clockIn = formatTime(session.clockInTime)
            val clockOut = session.clockOutTime?.let { formatTime(it) } ?: strings.inProgress()
            val durationText = if (session.clockOutTime != null) {
                CheckinnViewModel.formatDuration(session.durationMs)
            } else {
                val activeMs = uiState.currentTimeMs - session.clockInTime
                CheckinnViewModel.formatDuration(activeMs)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 序号标记
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${uiState.todayRecord.sessions.size - index}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFamily,
                        color = AppColors.primary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 时间区间 — 等宽字体
                Text(
                    text = "$clockIn → $clockOut",
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMonoFamily,
                    color = AppColors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 时长 — 等宽字体
                Text(
                    text = durationText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = JetBrainsMonoFamily,
                    color = AppColors.primaryLight,
                )
            }
            // 分隔线 (除最后一条)
            if (index < displayedSessions.size - 1) {
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

// ==================== 手动打卡按钮 ====================

@Composable
fun ManualCheckButtons(viewModel: CheckinnViewModel, strings: StringResources) {
    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
    ) {
        Text(
            text = strings.manualCheck(),
            fontSize = 12.sp,
            color = AppColors.textMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 上班按钮 - 渐变绿
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.primary, AppColors.primaryLight)
                        )
                    )
                    .clickable { viewModel.onNfcScanned(NfcScene.CLOCK_IN) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.clockIn(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // 下班按钮 - 渐变橙
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.clockOut, AppColors.accent)
                        )
                    )
                    .clickable { viewModel.onNfcScanned(NfcScene.CLOCK_OUT) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.clockOut(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

// ==================== NFC 写入区域 ====================

@Composable
fun NfcWriteSection(viewModel: CheckinnViewModel, uiState: CheckinnUiState, strings: StringResources) {
    GlassCardColumn(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
    ) {
        Text(
            text = strings.nfcSetup(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = strings.nfcDescription(),
            fontSize = 12.sp,
            color = AppColors.textMuted,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 写入上班卡 - 绿色描边
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, AppColors.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .background(AppColors.primary.copy(alpha = 0.08f))
                    .clickable { viewModel.enterWriteMode(NfcScene.CLOCK_IN) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.writeClockIn(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.primaryLight,
                )
            }

            // 写入下班卡 - 橙色描边
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, AppColors.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .background(AppColors.accent.copy(alpha = 0.08f))
                    .clickable { viewModel.enterWriteMode(NfcScene.CLOCK_OUT) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.writeClockOut(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.accentLight,
                )
            }
        }
    }
}

// ==================== NFC 写入对话框 ====================

@Composable
fun NfcWriteDialog(scene: NfcScene?, strings: StringResources, onDismiss: () -> Unit) {
    val sceneText = when (scene) {
        NfcScene.CLOCK_IN -> strings.clockIn()
        NfcScene.CLOCK_OUT -> strings.clockOut()
        null -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgMid,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                strings.nfcWriteDialogTitle(),
                color = AppColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.nfcWriteInstruction(),
                    textAlign = TextAlign.Center,
                    color = AppColors.textSecondary,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.nfcWriteScene(sceneText),
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primaryLight,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
        },
        confirmButton = {},
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

// ==================== Lottie 动画覆盖层 ====================

@Composable
fun LottieOverlay(animationType: AnimationType, strings: StringResources, onDismiss: () -> Unit) {
    val lottieFile = when (animationType) {
        AnimationType.CLOCK_IN -> "files/confetti-dots.lottie"
        AnimationType.CLOCK_OUT -> "files/confetti-dots.lottie"
        AnimationType.NONE -> return
    }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            archive = Res.readBytes(lottieFile)
        )
    }

    LaunchedEffect(animationType) {
        delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.7f),
                    ),
                )
            )
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                iterations = 1,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.height(20.dp))

            val msgColor = when (animationType) {
                AnimationType.CLOCK_IN -> AppColors.primaryLight
                AnimationType.CLOCK_OUT -> AppColors.accentLight
                AnimationType.NONE -> Color.White
            }

            Text(
                text = when (animationType) {
                    AnimationType.CLOCK_IN -> strings.animClockInSuccess()
                    AnimationType.CLOCK_OUT -> strings.animClockOutSuccess()
                    AnimationType.NONE -> ""
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = msgColor,
                letterSpacing = 1.sp,
            )
        }
    }
}

/** 将时间戳格式化为 HH:mm:ss */
expect fun formatTime(timestampMs: Long): String
