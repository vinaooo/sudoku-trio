package io.github.vinaooo.sudokutrio.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.vinaooo.sudokutrio.domain.model.ThemeColor
import io.github.vinaooo.sudokutrio.domain.model.ThemeMode

fun isDarkTheme(themeMode: ThemeMode, systemInDark: Boolean): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Dynamic (wallpaper) colors on Android 12+, the chosen [themeColor]'s otherwise. */
fun colorSchemeFor(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    themeColor: ThemeColor = ThemeColor.BLUE,
): ColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
} else {
    paletteScheme(themeColor, darkTheme)
}

/** Material 3 Expressive with the expressive motion scheme, plus the board's colors ([SudokuTrioThemeExtras]). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SudokuTrioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeColor: ThemeColor = ThemeColor.BLUE,
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeMode, isSystemInDarkTheme())
    val context = LocalContext.current
    val colorScheme = remember(context, darkTheme, dynamicColor, themeColor) {
        colorSchemeFor(context, darkTheme, dynamicColor, themeColor)
    }
    val boardColors = remember(colorScheme) { boardColorsFor(colorScheme) }
    CompositionLocalProvider(LocalBoardColors provides boardColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography(),
            content = content,
        )
    }
}

object SudokuTrioThemeExtras {
    val boardColors: BoardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBoardColors.current
}
