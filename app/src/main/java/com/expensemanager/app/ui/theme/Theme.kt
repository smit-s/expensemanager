package com.expensemanager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightAppColors = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    secondary = Color(0xFF0D47A1),
    background = Color(0xFFF5F9FF),
    surface = Color.White,
    onSurface = Color(0xFF111111)
)

private val DarkAppColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF062A45),
    secondary = Color(0xFF64B5F6),
    background = Color(0xFF0F141A),
    surface = Color(0xFF1A222C),
    onSurface = Color(0xFFEAF2FB)
)

@Composable
fun ExpenseManagerTheme(themeMode: String = "Light", content: @Composable () -> Unit) {
    val colors = if (themeMode.equals("Dark", ignoreCase = true)) DarkAppColors else LightAppColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}



