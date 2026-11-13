package com.example.gastify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GastifyColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = SecondaryPurple,

    background = AmoledBlack,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,

    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,

    error = ExpenseRed,
    onError = TextPrimary,

    outline = BorderDark
)

@Composable
fun GastifyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GastifyColorScheme,
        content = content
    )
}