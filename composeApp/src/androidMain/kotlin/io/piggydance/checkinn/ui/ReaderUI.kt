package io.piggydance.checkinn.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.piggydance.checkinn.nfc.NFCResult
import io.piggydance.checkinn.nfc.NFCStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


// 动画数字组件
@Composable
fun AnimatedCounter(count: Int) {
    val countAnimation = animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Text(
        text = countAnimation.value.toString(),
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

// 状态图标组件
@Composable
fun StatusIcon(status: NFCStatus): ImageVector {
    return when (status) {
        NFCStatus.ENABLED -> Icons.Filled.CheckCircle
        NFCStatus.DISABLED -> Icons.Filled.Close
        NFCStatus.NOT_SUPPORTED -> Icons.Filled.Info
    }
}

// 状态颜色组件
@Composable
fun StatusColor(status: NFCStatus): Color {
    return when (status) {
        NFCStatus.ENABLED -> Color(0xFF4CAF50)
        NFCStatus.DISABLED -> Color(0xFFF44336)
        NFCStatus.NOT_SUPPORTED -> Color(0xFF9E9E9E)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReaderUI(
    nfcStatus: NFCStatus,
    lastReadResult: NFCResult?,
    readHistory: List<NFCResult>,
    readCount: Int,
    onReadAgain: () -> Unit
) {
    val isEnabled = nfcStatus == NFCStatus.ENABLED

    // 为背景添加渐变色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA),
                        Color(0xFFE4EAF5)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 标题栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NFC 打卡应用",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 打卡计数卡片
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "总打卡次数",
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedCounter(count = readCount)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "继续打卡，保持记录！",
                            fontSize = 16.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }

            // NFC 状态卡片
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StatusColor(nfcStatus).copy(alpha = 0.1f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = StatusIcon(nfcStatus),
                                contentDescription = null,
                                tint = StatusColor(nfcStatus),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column() {
                            Text(
                                text = "NFC 状态",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (nfcStatus) {
                                    NFCStatus.ENABLED -> "NFC 已准备就绪，可进行打卡"
                                    NFCStatus.DISABLED -> "请在设置中启用 NFC"
                                    NFCStatus.NOT_SUPPORTED -> "此设备不支持 NFC 打卡功能"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }

            // 最近打卡结果
            lastReadResult?.let {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "最近打卡结果",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (it.success) "打卡成功" else "打卡失败",
                                    color = if (it.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // 解析卡片类型
                            val (cardType, payload) = it.data?.let { raw ->
                                val split = raw.split('|', limit = 2)
                                if (split.size == 2) {
                                    try {
                                        CardType.valueOf(split[0]) to split[1]
                                    } catch (_: Exception) {
                                        CardType.OTHER to raw
                                    }
                                } else CardType.OTHER to raw
                            } ?: (CardType.OTHER to "")

                            it.tagId?.let {
                                InfoRow(label = "标签 ID", value = it)
                            }
                            InfoRow(label = "卡片类型", value = cardType.displayName)
                            it.tagType?.let {
                                InfoRow(label = "标签类型", value = it)
                            }
                            if (payload.isNotEmpty()) {
                                InfoRow(label = "数据", value = payload)
                            }
                            it.errorMessage?.let {
                                InfoRow(label = "错误", value = it, isError = true)
                            }
                            InfoRow(
                                label = "时间",
                                value = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                            )
                        }
                    }
                }
            }
        }
    }
}

// 信息行组件
@Composable
fun InfoRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = if (isError) Color(0xFFF44336) else Color(0xFF333333),
            fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal
        )
    }
}
