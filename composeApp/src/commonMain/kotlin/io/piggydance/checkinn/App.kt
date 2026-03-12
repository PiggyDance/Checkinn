package io.piggydance.checkinn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkinn.composeapp.generated.resources.Res
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

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    BottomNav(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(viewModel = viewModel, uiState = uiState)
                        AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    }
                }
            }

            // Lottie 动画覆盖层 (全局覆盖)
            AnimatedVisibility(
                visible = uiState.showAnimation,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                LottieOverlay(
                    animationType = uiState.animationType,
                    onDismiss = { viewModel.dismissAnimation() },
                )
            }

            // NFC 写入提示对话框
            if (uiState.isWriteMode) {
                NfcWriteDialog(
                    scene = uiState.writeScene,
                    onDismiss = { viewModel.exitWriteMode() },
                )
            }
        }
    }
}

@Composable
fun BottomNav(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Text("🏠", fontSize = 20.sp) },
            label = { Text("打卡") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF4CAF50),
                selectedTextColor = Color(0xFF4CAF50),
                indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
            ),
        )
        NavigationBarItem(
            selected = currentTab == AppTab.HISTORY,
            onClick = { onTabSelected(AppTab.HISTORY) },
            icon = { Text("📊", fontSize = 20.sp) },
            label = { Text("记录") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF4CAF50),
                selectedTextColor = Color(0xFF4CAF50),
                indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
            ),
        )
    }
}

// ==================== 首页 ====================

@Composable
fun HomeScreen(viewModel: CheckinnViewModel, uiState: CheckinnUiState) {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Checkinn",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "NFC 智能打卡",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        StatusCard(uiState = uiState)

        Spacer(modifier = Modifier.height(20.dp))

        SessionsCard(uiState = uiState)

        Spacer(modifier = Modifier.height(20.dp))

        ManualCheckButtons(viewModel = viewModel)

        Spacer(modifier = Modifier.height(20.dp))

        NfcWriteSection(viewModel = viewModel, uiState = uiState)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==================== 首页组件 ====================

@Composable
fun StatusCard(uiState: CheckinnUiState) {
    val isWorking = uiState.todayRecord.hasActiveSession
    val bgColor = if (isWorking) Color(0xFF4CAF50) else Color(0xFF757575)
    val statusText = if (isWorking) "工作中" else "未上班"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isWorking) Color(0xFF81C784) else Color(0xFFBDBDBD))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Text(
                text = "今日累计: ${CheckinnViewModel.formatDuration(totalDuration)}",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
            )

            if (isWorking) {
                Spacer(modifier = Modifier.height(4.dp))
                val activeSession = uiState.todayRecord.activeSession
                if (activeSession != null) {
                    val activeMs = uiState.currentTimeMs - activeSession.clockInTime
                    Text(
                        text = "本段: ${CheckinnViewModel.formatDuration(activeMs)}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "打卡段数: ${uiState.todayRecord.sessions.size}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun SessionsCard(uiState: CheckinnUiState) {
    if (uiState.todayRecord.sessions.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今日工作时段",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            uiState.todayRecord.sessions.forEachIndexed { index, session ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val clockIn = formatTime(session.clockInTime)
                    val clockOut = session.clockOutTime?.let { formatTime(it) } ?: "进行中..."
                    val durationText = if (session.clockOutTime != null) {
                        CheckinnViewModel.formatDuration(session.durationMs)
                    } else {
                        val activeMs = uiState.currentTimeMs - session.clockInTime
                        CheckinnViewModel.formatDuration(activeMs)
                    }

                    Text(
                        text = "#${index + 1}  $clockIn → $clockOut",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = durationText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun ManualCheckButtons(viewModel: CheckinnViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "手动打卡 (调试用)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { viewModel.onNfcScanned(NfcScene.CLOCK_IN) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("上班打卡", color = Color.White)
                }

                Button(
                    onClick = { viewModel.onNfcScanned(NfcScene.CLOCK_OUT) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("下班打卡", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun NfcWriteSection(viewModel: CheckinnViewModel, uiState: CheckinnUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "NFC 贴纸设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "将打卡场景写入空白NFC贴纸",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(
                    onClick = { viewModel.enterWriteMode(NfcScene.CLOCK_IN) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("写入「上班」卡")
                }

                OutlinedButton(
                    onClick = { viewModel.enterWriteMode(NfcScene.CLOCK_OUT) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("写入「下班」卡")
                }
            }
        }
    }
}

@Composable
fun NfcWriteDialog(scene: NfcScene?, onDismiss: () -> Unit) {
    val sceneText = when (scene) {
        NfcScene.CLOCK_IN -> "上班打卡"
        NfcScene.CLOCK_OUT -> "下班打卡"
        null -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写入NFC贴纸") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "请将空白NFC贴纸贴近手机背面",
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "即将写入: 「$sceneText」",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun LottieOverlay(animationType: AnimationType, onDismiss: () -> Unit) {
    val lottieFile = when (animationType) {
        AnimationType.CLOCK_IN -> "files/confetti.lottie"
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
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                iterations = 1,
                modifier = Modifier.size(300.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (animationType) {
                    AnimationType.CLOCK_IN -> "上班打卡成功！"
                    AnimationType.CLOCK_OUT -> "下班打卡成功！"
                    AnimationType.NONE -> ""
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

/** 将时间戳格式化为 HH:mm:ss */
expect fun formatTime(timestampMs: Long): String
