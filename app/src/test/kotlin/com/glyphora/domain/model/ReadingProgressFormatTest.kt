package com.glyphora.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressFormatTest {

    @Test
    fun `page 2 sur 4 correspond a 50 pour cent`() {
        // L'ancien affichage montrait « 2/4 » dans le lecteur et « 25 % » dans la bibliothèque
        assertEquals(2, displayPageNumber(pageIndex = 1, totalPages = 4))
        assertEquals(50, progressPercent(pageIndex = 1, totalPages = 4))
        // Espace insécable (U+00A0) avant « % » : évite qu'il passe seul à la ligne
        assertEquals("Page 2 / 4 · 50 %", progressLabel(DocumentFormat.PDF, pageIndex = 1, totalPages = 4))
    }

    @Test
    fun `la derniere page vaut 100 pour cent`() {
        assertEquals(100, progressPercent(pageIndex = 3, totalPages = 4))
        assertEquals(1f, progressFraction(pageIndex = 3, totalPages = 4), 0f)
    }

    @Test
    fun `la premiere page n'est pas a zero pour cent`() {
        assertEquals(25, progressPercent(pageIndex = 0, totalPages = 4))
    }

    @Test
    fun `un index hors limites reste dans la plage affichable`() {
        assertEquals(4, displayPageNumber(pageIndex = 10, totalPages = 4))
        assertEquals(1, displayPageNumber(pageIndex = -3, totalPages = 4))
    }

    @Test
    fun `un total inconnu ne provoque ni division par zero ni pourcentage trompeur`() {
        assertEquals(0f, progressFraction(pageIndex = 0, totalPages = 0), 0f)
        assertEquals("Page 1", positionLabel(DocumentFormat.TXT, pageIndex = 0, totalPages = 0))
    }

    @Test
    fun `un EPUB parle de chapitres`() {
        assertEquals("Chapitre 3 / 10", positionLabel(DocumentFormat.EPUB, pageIndex = 2, totalPages = 10))
    }
}
