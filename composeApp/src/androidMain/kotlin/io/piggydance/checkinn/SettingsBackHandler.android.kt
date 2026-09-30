package io.piggydance.checkinn

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun SettingsBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
