package app.wireframephoto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import app.wireframephoto.ui.theme.WireframeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            WireframeTheme {
                WireframeAppUi(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Photos shared from Gallery ("공유 → 와이어프레임 포토") start or extend a collage. */
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
