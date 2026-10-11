package com.glyphora.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.glyphora.presentation.bookmarks.BookmarksScreen
import com.glyphora.presentation.library.LibraryScreen
import com.glyphora.presentation.reader.ReaderScreen
import com.glyphora.presentation.settings.SettingsScreen

@Composable
fun GlyphoraNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenDocument = { docId ->
                    navController.navigate(Screen.Reader.createRoute(docId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId").orEmpty()
            val startPage = backStackEntry.arguments?.getInt("page", -1)?.takeIf { it >= 0 }
            ReaderScreen(
                documentId = documentId,
                startPage = startPage,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenBookmarks = { navController.navigate(Screen.Bookmarks.route) }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpenBookmark = { documentId, pageIndex ->
                    // On repart de la bibliothèque pour ne pas empiler plusieurs lecteurs
                    navController.navigate(Screen.Reader.createRoute(documentId, pageIndex)) {
                        popUpTo(Screen.Library.route)
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
