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
private val NoorDisplayFamily = FontFamily.Serif

private val NoorTypography = Typography().let { defaults ->
    defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = NoorDisplayFamily, fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
        displayMedium = defaults.displayMedium.copy(fontFamily = NoorDisplayFamily, fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
        displaySmall = defaults.displaySmall.copy(fontFamily = NoorDisplayFamily, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = NoorDisplayFamily, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = NoorDisplayFamily, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = NoorDisplayFamily, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = defaults.titleLarge.copy(fontFamily = NoorDisplayFamily, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = defaults.titleMedium.copy(fontFamily = NoorFontFamily, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = defaults.titleSmall.copy(fontFamily = NoorFontFamily, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = NoorFontFamily, fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = NoorFontFamily, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        bodySmall = defaults.bodySmall.copy(fontFamily = NoorFontFamily, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
        labelLarge = defaults.labelLarge.copy(fontFamily = NoorFontFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = defaults.labelMedium.copy(fontFamily = NoorFontFamily, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        labelSmall = defaults.labelSmall.copy(fontFamily = NoorFontFamily, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
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
