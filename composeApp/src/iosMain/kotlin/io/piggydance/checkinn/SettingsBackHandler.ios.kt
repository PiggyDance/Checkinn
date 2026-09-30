package io.piggydance.checkinn

import androidx.compose.runtime.Composable

@Composable
actual fun SettingsBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the explicit in-app back button.
}
