package com.glyphora.domain.model

import kotlin.math.roundToInt

/*
 * Convention unique pour la progression de lecture.
 *
 * - Les données stockées (Document.currentPage, Bookmark.pageIndex) sont des index à partir de 0.
 * - Ce que l'utilisateur voit est la position à partir de 1 : « Page 2 / 4 ».
 * - Le pourcentage est calculé sur cette même position : page 2 sur 4 = 50 %, dernière page = 100 %.
 *
 * Le lecteur, la bibliothèque et la liste des marque-pages passent tous par ces fonctions.
 */

/** Numéro de page affiché (à partir de 1) pour un index à partir de 0. */
fun displayPageNumber(pageIndex: Int, totalPages: Int): Int =
    if (totalPages <= 0) 1 else (pageIndex + 1).coerceIn(1, totalPages)

/** Fraction de lecture entre 0 et 1, cohérente avec le numéro de page affiché. */
fun progressFraction(pageIndex: Int, totalPages: Int): Float =
    if (totalPages <= 0) 0f else displayPageNumber(pageIndex, totalPages).toFloat() / totalPages

fun progressPercent(pageIndex: Int, totalPages: Int): Int =
    (progressFraction(pageIndex, totalPages) * 100).roundToInt()

/** « Page 2 / 4 » (ou « Chapitre 2 / 4 » pour un EPUB). */
fun positionLabel(format: DocumentFormat, pageIndex: Int, totalPages: Int): String {
    val unit = if (format == DocumentFormat.EPUB) "Chapitre" else "Page"
    val number = displayPageNumber(pageIndex, totalPages)
    return if (totalPages > 0) "$unit $number / $totalPages" else "$unit $number"
}

/** « Page 2 / 4 · 50 % » */
fun progressLabel(format: DocumentFormat, pageIndex: Int, totalPages: Int): String =
    "${positionLabel(format, pageIndex, totalPages)} · ${progressPercent(pageIndex, totalPages)} %"
