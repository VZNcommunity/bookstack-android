package com.vzith.bookstack.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vzith.bookstack.ui.library.LibraryScreen
import com.vzith.bookstack.ui.editor.EditorScreen
import com.vzith.bookstack.ui.settings.SettingsScreen

/**
 * BookStack Android App - Navigation Graph (2026-01-05)
 * Updated: 2026-01-11 - Added adaptive two-pane layout for tablets
 */
sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Editor : Screen("editor/{pageId}") {
        fun createRoute(pageId: Int) = "editor/$pageId"
    }
}

/**
 * Adaptive app container that switches between phone and tablet layouts (2026-01-11)
 */
@Composable
fun AdaptiveBookStackApp(
    isExpandedScreen: Boolean,
    startDestination: String
) {
    if (isExpandedScreen) {
        TwoPaneLayout(startDestination = startDestination)
    } else {
        val navController = rememberNavController()
        BookStackNavGraph(
            navController = navController,
            startDestination = startDestination
        )
    }
}

/**
 * Two-pane layout for tablets (2026-01-11)
 * Left pane: Library/book list (fixed width)
 * Right pane: Editor or placeholder
 */
@Composable
fun TwoPaneLayout(
    startDestination: String
) {
    // Selected page ID for right pane
    var selectedPageId by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(startDestination == Screen.Settings.route) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left pane: Library (1/3 width, min 320dp)
        Box(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (showSettings) {
                SettingsScreen(
                    onNavigateBack = { showSettings = false }
                )
            } else {
                LibraryScreen(
                    onPageClick = { pageId ->
                        selectedPageId = pageId
                    },
                    onSettingsClick = {
                        showSettings = true
                    }
                )
            }
        }

        // Divider
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right pane: Editor or placeholder (remaining width)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedPageId != null) {
                EditorScreen(
                    pageId = selectedPageId!!,
                    onNavigateBack = {
                        selectedPageId = null
                    }
                )
            } else {
                // Placeholder when no page selected
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Select a page to edit",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BookStackNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Library.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onPageClick = { pageId ->
                    navController.navigate(Screen.Editor.createRoute(pageId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("pageId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getInt("pageId") ?: 0
            EditorScreen(
                pageId = pageId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
