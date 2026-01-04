package com.vzith.bookstack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vzith.bookstack.ui.library.LibraryScreen
import com.vzith.bookstack.ui.editor.EditorScreen
import com.vzith.bookstack.ui.settings.SettingsScreen

/**
 * BookStack Android App - Navigation Graph (2026-01-05)
 */
sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Editor : Screen("editor/{pageId}") {
        fun createRoute(pageId: Int) = "editor/$pageId"
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
