package app.wireframephoto.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wireframephoto.core.Templates
import app.wireframephoto.ui.editor.EditorScreen
import app.wireframephoto.ui.home.HomeScreen

val ImagesOnly = PickVisualMediaRequest(PickVisualMedia.ImageOnly)

/** Keeps Photo Picker grants across process death so a restored collage can still read its photos. */
fun Context.persistReadAccess(uris: List<Uri>) {
    uris.forEach { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
}

@Composable
fun WireframeAppUi(viewModel: EditorViewModel) {
    val editing by viewModel.isEditing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Purpose chosen on the home screen, kept while the Photo Picker is open.
    var pendingPreset by rememberSaveable { mutableStateOf<String?>(null) }

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
        HomeScreen(onPurpose = { purpose ->
            pendingPreset = purpose.presetId
            pickPhotos.launch(ImagesOnly)
        })
    }
}
