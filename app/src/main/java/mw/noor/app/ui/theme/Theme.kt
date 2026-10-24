package mw.noor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun NoorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoorLightColorScheme,
        content = content,
    )
}
