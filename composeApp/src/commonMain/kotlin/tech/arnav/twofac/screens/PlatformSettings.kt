package tech.arnav.twofac.screens

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSettingsContent(onQuit: (() -> Unit)? = null)
