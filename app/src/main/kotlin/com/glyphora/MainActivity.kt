package com.glyphora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.glyphora.core.theme.GlyphoraTheme
import com.glyphora.presentation.navigation.GlyphoraNavGraph
import com.glyphora.presentation.navigation.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = (application as GlyphoraApplication).documentRepository
            val settings by repository.getReaderSettings().collectAsState(
                initial = com.glyphora.domain.model.ReaderSettings()
            )
            val navController = rememberNavController()

            // Gestion réactive sécurisée de l'ouverture de fichier direct (Intent ACTION_VIEW)
            LaunchedEffect(intent?.data) {
                intent?.data?.let { uri ->
                    handleIncomingUri(uri) { docId ->
                        navController.navigate(Screen.Reader.createRoute(docId))
                    }
                }
            }

            GlyphoraTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GlyphoraNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingUri(uri: Uri, onReady: (String) -> Unit) {
        val repository = (application as GlyphoraApplication).documentRepository
        lifecycleScope.launch {
            val result = repository.importDocument(uri)
            result.getOrNull()?.let { doc ->
                onReady(doc.id)
            }
        }
    }
}
