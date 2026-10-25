package mw.noor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

private val NoorLightColorScheme = lightColorScheme(
    primary = NoorColors.Moss,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = NoorColors.Mint,
    onSecondary = NoorColors.MossDark,
    tertiary = NoorColors.Sun,
    onTertiary = NoorColors.Ink,
    background = NoorColors.Canvas,
    onBackground = NoorColors.Ink,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = NoorColors.Ink,
    surfaceVariant = NoorColors.Mint,
    onSurfaceVariant = NoorColors.MossDark,
)

private val NoorTypography = Typography().let { defaults ->
    defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontSize = 48.sp, lineHeight = 54.sp),
        displayMedium = defaults.displayMedium.copy(fontSize = 40.sp, lineHeight = 46.sp),
        displaySmall = defaults.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp),
        headlineLarge = defaults.headlineLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
        headlineMedium = defaults.headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = defaults.headlineSmall.copy(fontSize = 21.sp, lineHeight = 27.sp),
        titleLarge = defaults.titleLarge.copy(fontSize = 19.sp, lineHeight = 25.sp),
        titleMedium = defaults.titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = defaults.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = defaults.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = defaults.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = defaults.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = defaults.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = defaults.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
    )
}

@Composable
fun NoorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoorLightColorScheme,
        typography = NoorTypography,
        content = content,
    )
}
