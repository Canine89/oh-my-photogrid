package app.wireframephoto.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wireframephoto.core.Templates
import app.wireframephoto.data.AvailableUpdate
import app.wireframephoto.data.UpdateChecker
import app.wireframephoto.data.UpdateInstaller
import kotlinx.coroutines.launch
import app.wireframephoto.ui.editor.EditorScreen
import app.wireframephoto.ui.home.HomeScreen
import app.wireframephoto.ui.theme.AppTheme

val ImagesOnly = PickVisualMediaRequest(PickVisualMedia.ImageOnly)

/** Keeps Photo Picker grants across process death so a restored collage can still read its photos. */
fun Context.persistReadAccess(uris: List<Uri>) {
    uris.forEach { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
}

@Composable
fun WireframeAppUi(viewModel: EditorViewModel, theme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    val editing by viewModel.isEditing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Purpose chosen on the home screen, kept while the Photo Picker is open.
    var pendingPreset by rememberSaveable { mutableStateOf<String?>(null) }
    val updates = remember { UpdateChecker(context.applicationContext) }
    var update by remember { mutableStateOf(updates.cached()) }
    LaunchedEffect(updates) { update = updates.check() }
    val updatePhase by UpdateInstaller.phase.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    fun installUpdate(u: AvailableUpdate) {
        scope.launch { UpdateInstaller.downloadAndInstall(context, u.apkUrl) }
    }
    // First update only: Android asks once to "allow from this source". Send the user there
    // before downloading, and carry on automatically when they come back with it allowed.
    val allowInstalls = rememberLauncherForActivityResult(StartActivityForResult()) {
        val u = update
        if (u != null && context.packageManager.canRequestPackageInstalls()) installUpdate(u)
    }

    val pickPhotos = rememberLauncherForActivityResult(PickMultipleVisualMedia(Templates.MAX_PHOTOS)) { uris ->
        val preset = pendingPreset
        pendingPreset = null
        if (uris.isNotEmpty() && preset != null) {
            context.persistReadAccess(uris)
            viewModel.startNew(uris, preset)
        }
    }

    if (editing) {
        EditorScreen(viewModel)
    } else {
        HomeScreen(
            theme = theme,
            onThemeChange = onThemeChange,
            update = update,
            updatePhase = updatePhase,
            onUpdate = { u ->
                if (context.packageManager.canRequestPackageInstalls()) {
                    installUpdate(u)
                } else {
                    allowInstalls.launch(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                    )
                }
            },
            onOpenReleasePage = { u ->
                // Fallback when the in-app install fails: the release page in a browser.
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.pageUrl)))
                } catch (_: ActivityNotFoundException) {
                }
            },
            onDismissUpdate = { u -> updates.dismiss(u); update = null },
            onPurpose = { purpose ->
                pendingPreset = purpose.presetId
                pickPhotos.launch(ImagesOnly)
            },
        )
    }
}
