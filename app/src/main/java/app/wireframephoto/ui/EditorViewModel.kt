package app.wireframephoto.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.wireframephoto.WireframeApp
import app.wireframephoto.core.AspectPresets
import app.wireframephoto.core.CellTransform
import app.wireframephoto.core.CollageState
import app.wireframephoto.core.CollageStyle
import app.wireframephoto.core.Templates
import app.wireframephoto.core.shuffled
import app.wireframephoto.data.ExportFormat
import app.wireframephoto.data.ImageSaver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Running(val progress: Float) : ExportStatus
    data class Done(val uri: Uri, val mimeType: String, val width: Int, val height: Int, val isWallpaper: Boolean) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

class EditorViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {

    private val wireframeApp = app as WireframeApp
    val loader get() = wireframeApp.loader

    /** The document survives fold/unfold, rotation and process death via SavedStateHandle. */
    val state: StateFlow<CollageState> = saved.getStateFlow(KEY_STATE, CollageState())
    val isEditing: StateFlow<Boolean> = saved.getStateFlow(KEY_EDITING, false)
    val selectedCell: StateFlow<Int?> = saved.getStateFlow(KEY_SELECTED, null)

    private val _export = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val export: StateFlow<ExportStatus> = _export.asStateFlow()

    private val undoStack = ArrayDeque<CollageState>()
    private val redoStack = ArrayDeque<CollageState>()
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var gestureSnapshot: CollageState? = null
    private var exportJob: Job? = null

    private val current get() = state.value

    /**
     * Starts a new collage on the canvas of [presetId]. The default layout fits the photo count and
     * canvas shape; with no photos, an empty [emptyCells]-cell layout is shown for tap-to-fill.
     */
    fun startNew(uris: List<Uri>, presetId: String, emptyCells: Int = 4) {
        val preset = AspectPresets.byId(presetId)
        val base = CollageState(
            templateId = Templates.defaultFor(if (uris.isEmpty()) emptyCells else uris.size, preset.aspect).id,
            presetId = preset.id,
        )
        undoStack.clear()
        redoStack.clear()
        set(base.addPhotos(uris.map(Uri::toString)))
        saved[KEY_SELECTED] = null
        saved[KEY_EDITING] = true
        updateHistoryFlags()
    }

    fun close() {
        saved[KEY_EDITING] = false
        saved[KEY_SELECTED] = null
        _export.value = ExportStatus.Idle
    }

    fun select(cell: Int?) {
        saved[KEY_SELECTED] = cell
    }

    fun addPhotos(uris: List<Uri>) = commit(current.addPhotos(uris.map(Uri::toString)))

    fun replacePhoto(cell: Int, uri: Uri) = commit(current.setPhoto(cell, uri.toString()))

    fun clearPhoto(cell: Int) = commit(current.clearPhoto(cell))

    fun swap(a: Int, b: Int) {
        commit(current.swap(a, b))
        select(b)
    }

    fun resetTransform(cell: Int) = commit(current.resetTransform(cell))

    fun selectTemplate(id: String) {
        commit(current.withTemplate(id))
        if ((selectedCell.value ?: 0) >= current.template.count) select(null)
    }

    fun selectPreset(id: String) = commit(current.withPreset(id))

    /** One undoable step, so a shuffle the user doesn't like is a single tap away from reverting. */
    fun shuffle() = commit(current.shuffled())

    /** Slider drags update live; [finished] records one undo step for the whole drag. */
    fun updateStyle(style: CollageStyle, finished: Boolean) {
        if (gestureSnapshot == null) gestureSnapshot = current
        set(current.withStyle(style))
        if (finished) endGesture()
    }

    fun beginGesture() {
        if (gestureSnapshot == null) gestureSnapshot = current
    }

    fun updateTransform(cell: Int, transform: CellTransform) {
        set(current.setTransform(cell, transform))
    }

    fun endGesture() {
        val before = gestureSnapshot ?: return
        gestureSnapshot = null
        if (before != current) pushUndo(before)
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current)
        set(prev)
        updateHistoryFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current)
        set(next)
        updateHistoryFlags()
    }

    fun export(scale: Float, format: ExportFormat) {
        if (exportJob?.isActive == true) return
        val snapshot = current
        val (w, h) = snapshot.preset.sizeAt(scale)
        exportJob = viewModelScope.launch {
            _export.value = ExportStatus.Running(0f)
            try {
                val bitmap = wireframeApp.renderer.render(snapshot, w, h) { p -> _export.value = ExportStatus.Running(p) }
                val uri = try {
                    ImageSaver.save(getApplication(), bitmap, format)
                } finally {
                    bitmap.recycle()
                }
                _export.value = ExportStatus.Done(uri, format.mimeType, w, h, snapshot.preset.isWallpaper)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Export failed", e)
                _export.value = ExportStatus.Failed(e.message ?: e.javaClass.simpleName)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Export OOM", e)
                _export.value = ExportStatus.Failed("메모리가 부족합니다. 더 작은 크기로 저장해 보세요.")
            }
        }
    }

    fun dismissExport() {
        exportJob?.cancel()
        _export.value = ExportStatus.Idle
    }

    private fun commit(next: CollageState) {
        if (next == current) return
        pushUndo(current)
        set(next)
    }

    private fun pushUndo(state: CollageState) {
        undoStack.addLast(state)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun set(next: CollageState) {
        saved[KEY_STATE] = next
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    companion object {
        private const val TAG = "EditorViewModel"
        private const val KEY_STATE = "collage"
        private const val KEY_EDITING = "editing"
        private const val KEY_SELECTED = "selected"
        const val MAX_HISTORY = 50
    }
}
