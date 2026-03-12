package io.piggydance.checkinn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ========== 色板 ==========
// 深色背景 + 薄荷绿主色 + 琥珀色强调，灵感来自 Financial Dashboard palette

object AppColors {
    // 背景渐变
    val bgTop = Color(0xFF0B1120)         // 深蓝黑
    val bgBottom = Color(0xFF0F1A2E)      // 深靛蓝
    val bgMid = Color(0xFF101B2E)         // 中间过渡

    // 主色系 - 薄荷绿
    val primary = Color(0xFF22C55E)       // 鲜明绿
    val primaryLight = Color(0xFF4ADE80)  // 浅绿
    val primaryDim = Color(0xFF16A34A)    // 深绿
    val primarySurface = Color(0xFF22C55E).copy(alpha = 0.08f) // 绿色底色

    // 强调色 - 琥珀/橙
    val accent = Color(0xFFF59E0B)        // 琥珀
    val accentLight = Color(0xFFFBBF24)

    // 状态色
    val working = Color(0xFF22C55E)       // 工作中-绿
    val idle = Color(0xFF64748B)          // 未工作-灰蓝
    val clockOut = Color(0xFFF97316)      // 下班-橙

    // 文本
    val textPrimary = Color(0xFFF1F5F9)   // 近白
    val textSecondary = Color(0xFF94A3B8) // 柔灰蓝
    val textMuted = Color(0xFF64748B)     // 暗灰蓝

    // 毛玻璃
    val glassBg = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val glassBgHover = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val glassBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.15f)

    // 分隔线
    val divider = Color(0xFFFFFFFF).copy(alpha = 0.06f)
}

// ========== 暗色 Material 色板 ==========

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.primary,
    onPrimary = Color.White,
    secondary = AppColors.accent,
    onSecondary = Color.White,
    surface = AppColors.bgTop,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.glassBg,
    onSurfaceVariant = AppColors.textSecondary,
    primaryContainer = AppColors.primary.copy(alpha = 0.15f),
    onPrimaryContainer = AppColors.primaryLight,
    tertiaryContainer = AppColors.accent.copy(alpha = 0.15f),
    onTertiaryContainer = AppColors.accentLight,
    background = AppColors.bgTop,
    onBackground = AppColors.textPrimary,
    outline = AppColors.glassBorder,
    outlineVariant = AppColors.divider,
)

// ========== 排版 ==========

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        color = AppColors.textPrimary,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
        color = AppColors.textPrimary,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = AppColors.textPrimary,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = AppColors.textPrimary,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = AppColors.textSecondary,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = AppColors.textSecondary,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = AppColors.textMuted,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = AppColors.textPrimary,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = AppColors.textSecondary,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = AppColors.textMuted,
    ),
)

// ========== 主题入口 ==========

@Composable
fun CheckinnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}

// ========== 渐变背景 ==========

val AppBackgroundBrush = Brush.linearGradient(
    colors = listOf(AppColors.bgTop, AppColors.bgMid, AppColors.bgBottom),
    start = Offset(0f, 0f),
    end = Offset(0f, 2400f),
)

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackgroundBrush),
        content = content,
    )
}

// ========== 毛玻璃卡片 ==========

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.06f,
    borderAlpha: Float = 0.10f,
    contentPadding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = glassAlpha))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassCardColumn(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.06f,
    borderAlpha: Float = 0.10f,
    contentPadding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = glassAlpha))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        Column {
            content()
        }
    }
}

// ========== 渐变按钮用 Brush ==========

val PrimaryGradient = Brush.linearGradient(
    colors = listOf(AppColors.primary, AppColors.primaryLight),
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(AppColors.clockOut, AppColors.accent),
)
