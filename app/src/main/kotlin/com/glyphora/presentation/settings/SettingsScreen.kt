package com.glyphora.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glyphora.R
import com.glyphora.domain.model.ReaderFontFamily
import com.glyphora.domain.model.ReaderThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sélection du Thème
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mode de couleur",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.themeMode == ReaderThemeMode.SYSTEM,
                            onClick = { viewModel.updateThemeMode(ReaderThemeMode.SYSTEM) },
                            label = { Text("Système") }
                        )
                        FilterChip(
                            selected = settings.themeMode == ReaderThemeMode.LIGHT,
                            onClick = { viewModel.updateThemeMode(ReaderThemeMode.LIGHT) },
                            label = { Text("Clair") }
                        )
                        FilterChip(
                            selected = settings.themeMode == ReaderThemeMode.DARK,
                            onClick = { viewModel.updateThemeMode(ReaderThemeMode.DARK) },
                            label = { Text("Sombre") }
                        )
                        FilterChip(
                            selected = settings.themeMode == ReaderThemeMode.SEPIA,
                            onClick = { viewModel.updateThemeMode(ReaderThemeMode.SEPIA) },
                            label = { Text("Sépia") }
                        )
                    }
                }
            }

            // Famille de police
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_font_family),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.fontFamily == ReaderFontFamily.SERIF,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.SERIF) },
                            label = { Text("Serif") }
                        )
                        FilterChip(
                            selected = settings.fontFamily == ReaderFontFamily.SANS_SERIF,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.SANS_SERIF) },
                            label = { Text("Sans-Serif") }
                        )
                        FilterChip(
                            selected = settings.fontFamily == ReaderFontFamily.MONOSPACE,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.MONOSPACE) },
                            label = { Text("Monospace") }
                        )
                    }
                }
            }

            // Taille du texte
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_font_size),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = "${settings.fontSizeSp.toInt()} sp")
                    }
                    Slider(
                        value = settings.fontSizeSp,
                        onValueChange = { viewModel.updateFontSize(it) },
                        valueRange = 12f..32f,
                        steps = 9
                    )
                }
            }

            // Interligne
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_line_height),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = "%.1fx".format(settings.lineHeightMultiplier))
                    }
                    Slider(
                        value = settings.lineHeightMultiplier,
                        onValueChange = { viewModel.updateLineHeight(it) },
                        valueRange = 1.2f..2.2f,
                        steps = 4
                    )
                }
            }

            // Marges horizontales
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Marges latérales",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = "${settings.horizontalMarginDp} dp")
                    }
                    Slider(
                        value = settings.horizontalMarginDp.toFloat(),
                        onValueChange = { viewModel.updateMargin(it.toInt()) },
                        valueRange = 8f..36f,
                        steps = 6
                    )
                }
            }
        }
    }
}
