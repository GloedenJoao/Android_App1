package com.example.tutorial1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.tutorial1.ui.theme.PositiveGreen
import com.example.tutorial1.ui.theme.NegativeRed
import com.example.tutorial1.ui.theme.DarkSurface
import com.example.tutorial1.ui.theme.TextOnDark

private val DarkColorScheme = darkColorScheme(
    primary = PositiveGreen,
    secondary = NegativeRed,
    tertiary = PositiveGreen,
    background = DarkSurface,
    surface = DarkSurface,
    onPrimary = TextOnDark,
    onSecondary = TextOnDark,
    onSurface = TextOnDark,
    onBackground = TextOnDark
)

@Composable
fun Tutorial1Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}