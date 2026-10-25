package mw.noor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mw.noor.app.R

private val NoorLightColorScheme = lightColorScheme(
    primary = NoorColors.Moss,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = NoorColors.Mint,
    onSecondary = NoorColors.MossDark,
    tertiary = NoorColors.MossDark,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = NoorColors.Canvas,
    onBackground = NoorColors.Ink,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = NoorColors.Ink,
    surfaceVariant = NoorColors.Mint,
    onSurfaceVariant = NoorColors.MossDark,
)

private val NoorFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)

private val NoorTypography = Typography(defaultFontFamily = NoorFontFamily).let { defaults ->
    defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
        displayMedium = defaults.displayMedium.copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
        displaySmall = defaults.displaySmall.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
        headlineLarge = defaults.headlineLarge.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = defaults.headlineMedium.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = defaults.headlineSmall.copy(fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = defaults.titleLarge.copy(fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = defaults.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = defaults.titleSmall.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = defaults.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        bodySmall = defaults.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
        labelLarge = defaults.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = defaults.labelMedium.copy(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        labelSmall = defaults.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
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
