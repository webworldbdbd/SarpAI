package com.sarpai.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF22D3EE), onPrimary = Color(0xFF00212A), secondary = Color(0xFF818CF8),
    background = Color(0xFF0A1030), surface = Color(0xFF111A45), surfaceVariant = Color(0xFF1A2560),
    onBackground = Color(0xFFE8EEFF), onSurface = Color(0xFFE8EEFF)
)
private val Light = lightColorScheme(
    primary = Color(0xFF0B6FD6), secondary = Color(0xFF6366F1),
    background = Color(0xFFF6F8FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE3E9FF)
)

@Composable
fun SarpTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
