package com.glyphora.presentation.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object Reader : Screen("reader/{documentId}") {
        fun createRoute(documentId: String): String = "reader/$documentId"
    }
}
