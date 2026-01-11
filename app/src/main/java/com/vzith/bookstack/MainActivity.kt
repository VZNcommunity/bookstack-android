package com.vzith.bookstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vzith.bookstack.ui.navigation.AdaptiveBookStackApp
import com.vzith.bookstack.ui.navigation.Screen
import com.vzith.bookstack.ui.theme.BookStackTheme
import com.vzith.bookstack.util.KeystoreManager

/**
 * BookStack Android App - MainActivity (2026-01-05)
 * Updated: 2026-01-11 - Added theme mode support, tablet layout
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keystoreManager = BookStackApplication.instance.keystoreManager
        val startDestination = if (keystoreManager.isConfigured()) {
            Screen.Library.route
        } else {
            Screen.Settings.route
        }

        setContent {
            // Calculate window size class for adaptive layout (2026-01-11)
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            // Theme state that can be updated from Settings (2026-01-11)
            var themeMode by remember { mutableStateOf(keystoreManager.getThemeMode()) }
            val isSystemDark = isSystemInDarkTheme()

            val useDarkTheme = when (themeMode) {
                KeystoreManager.ThemeMode.SYSTEM -> isSystemDark
                KeystoreManager.ThemeMode.LIGHT -> false
                KeystoreManager.ThemeMode.DARK -> true
            }

            // Provide theme change callback via CompositionLocal
            CompositionLocalProvider(
                LocalThemeMode provides themeMode,
                LocalOnThemeModeChange provides { newMode ->
                    keystoreManager.saveThemeMode(newMode)
                    themeMode = newMode
                },
                LocalIsExpandedScreen provides isExpandedScreen
            ) {
                BookStackTheme(darkTheme = useDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AdaptiveBookStackApp(
                            isExpandedScreen = isExpandedScreen,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}

// CompositionLocals for theme and layout (2026-01-11)
val LocalThemeMode = staticCompositionLocalOf { KeystoreManager.ThemeMode.SYSTEM }
val LocalOnThemeModeChange = staticCompositionLocalOf<(KeystoreManager.ThemeMode) -> Unit> { {} }
val LocalIsExpandedScreen = staticCompositionLocalOf { false }
