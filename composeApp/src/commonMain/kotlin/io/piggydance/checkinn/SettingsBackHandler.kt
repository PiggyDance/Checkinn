package io.piggydance.checkinn

import androidx.compose.runtime.Composable

@Composable
expect fun SettingsBackHandler(enabled: Boolean, onBack: () -> Unit)
