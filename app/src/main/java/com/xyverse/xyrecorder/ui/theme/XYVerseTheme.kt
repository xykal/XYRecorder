package com.xyverse.xyrecorder.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun XYVerseTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = XYVerseColors.PrimaryStart,
        onPrimary = XYVerseColors.TextPrimary,
        primaryContainer = XYVerseColors.PrimaryEnd,
        secondary = XYVerseColors.ElectricBlue,
        tertiary = XYVerseColors.NeonPurple,
        background = XYVerseColors.BackgroundDark,
        surface = XYVerseColors.BackgroundCard,
        onBackground = XYVerseColors.TextPrimary,
        onSurface = XYVerseColors.TextPrimary,
        error = XYVerseColors.Error
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// Custom Components - XYVerse Style

@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(XYVerseColors.PrimaryStart, XYVerseColors.PrimaryEnd)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = XYVerseColors.BackgroundElevated
        ),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        XYVerseColors.SurfaceGlass,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                color = XYVerseColors.BackgroundCard,
                shape = RoundedCornerShape(24.dp)
            ),
        content = content
    )
}

@Composable
fun NeonGlowBox(
    modifier: Modifier = Modifier,
    glowColor: Color = XYVerseColors.PrimaryStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = glowColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = XYVerseColors.BackgroundElevated,
                shape = RoundedCornerShape(20.dp)
            ),
        content = content
    )
}
