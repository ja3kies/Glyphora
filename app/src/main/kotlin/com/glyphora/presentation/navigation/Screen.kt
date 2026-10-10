package com.glyphora.presentation.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object Bookmarks : Screen("bookmarks")
    data object Reader : Screen("reader/{documentId}?initialPage={initialPage}") {
        fun createRoute(documentId: String, initialPage: Int? = null): String =
            "reader/$documentId?initialPage=${initialPage ?: -1}"
    }
}
