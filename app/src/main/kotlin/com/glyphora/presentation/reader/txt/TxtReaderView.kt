package com.glyphora.presentation.reader.txt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphora.domain.model.ReaderFontFamily
import com.glyphora.domain.model.ReaderSettings

@Composable
fun TxtReaderView(
    pages: List<String>,
    initialPage: Int = 0,
    settings: ReaderSettings,
    onPageChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(initialPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentPage) {
        scrollState.scrollTo(0)
        onPageChanged(currentPage, pages.size.coerceAtLeast(1))
    }

    val fontFamily = remember(settings.fontFamily) {
        when (settings.fontFamily) {
            ReaderFontFamily.SERIF -> FontFamily.Serif
            ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
            ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = settings.horizontalMarginDp.dp, vertical = 20.dp)
                .padding(bottom = 90.dp)
        ) {
            val content = pages.getOrNull(currentPage) ?: ""
            Text(
                text = content,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // Contrôleur de pagination inférieur
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Page précédente")
                }

                if (pages.size > 1) {
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { currentPage = it.toInt() },
                        valueRange = 0f..(pages.size - 1).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }

                Text(
                    text = "${currentPage + 1}/${pages.size.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelMedium
                )

                IconButton(
                    onClick = { if (currentPage < pages.size - 1) currentPage++ },
                    enabled = currentPage < pages.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Page suivante")
                }
            }
        }
    }
}
