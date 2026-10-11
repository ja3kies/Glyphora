package com.glyphora.presentation.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object Bookmarks : Screen("bookmarks")
    data object Reader : Screen("reader/{documentId}?page={page}") {
        /** [page] : index (à partir de 0) de la page à ouvrir ; null = reprendre là où la lecture s'est arrêtée. */
        fun createRoute(documentId: String, page: Int? = null): String =
            if (page == null) "reader/$documentId" else "reader/$documentId?page=$page"
    }
}
