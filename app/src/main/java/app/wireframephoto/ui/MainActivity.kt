package app.wireframephoto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.IntentCompat
import app.wireframephoto.ui.theme.AppTheme
import app.wireframephoto.ui.theme.WireframeTheme
import app.wireframephoto.ui.theme.loadAppTheme
import app.wireframephoto.ui.theme.palette
import app.wireframephoto.ui.theme.saveAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val initialTheme = loadAppTheme()
        applySystemBars(initialTheme)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            LaunchedEffect(theme) { applySystemBars(theme) }
            WireframeTheme(theme) {
                WireframeAppUi(viewModel, theme, onThemeChange = { theme = it; saveAppTheme(it) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Bar icons contrast with the theme; the window color matches it so folding doesn't flash. */
    private fun applySystemBars(theme: AppTheme) {
        val style = if (theme.palette().isLight) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        window.setBackgroundDrawable(ColorDrawable(theme.palette().bg.toArgb()))
    }

    /** Photos shared from Gallery ("공유 → oh-my-photogrid") start or extend a collage. */
    private fun handleShare(intent: Intent?) {
        val uris: List<Uri> = when (intent?.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        if (viewModel.isEditing.value) {
            viewModel.addPhotos(uris)
        } else {
            // Shared from Gallery: default to the wallpaper of the screen the user is holding.
            val landscape = resources.configuration.screenWidthDp > resources.configuration.screenHeightDp
            viewModel.startNew(uris, if (landscape) Purposes.FoldMain.presetId else Purposes.FoldCover.presetId)
        }
    }
}
