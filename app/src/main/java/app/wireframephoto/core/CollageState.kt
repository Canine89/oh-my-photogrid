package app.wireframephoto.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** A photo placed in the collage. Slot i is shown in template cell i. */
@Parcelize
data class Slot(
    val uri: String? = null,
    val transform: CellTransform = CellTransform.Identity,
) : Parcelable

/** Immutable collage document. All edits return a new instance, which makes undo trivial. */
@Parcelize
data class CollageState(
    val slots: List<Slot> = emptyList(),
    val templateId: String = Templates.defaultFor(1).id,
    val presetId: String = AspectPresets.Default.id,
    val style: CollageStyle = CollageStyle(),
) : Parcelable {

    val template: Template get() = Templates.byId(templateId) ?: Templates.defaultFor(1)
    val preset: AspectPreset get() = AspectPresets.byId(presetId)
    val photoCount: Int get() = slots.count { it.uri != null }

    fun slot(index: Int): Slot = slots.getOrNull(index) ?: Slot()

    fun withTemplate(id: String): CollageState {
        val t = Templates.byId(id) ?: return this
        // Photos beyond the new cell count stay in the list so switching back restores them.
        return copy(templateId = t.id, slots = packed(slots, t.count))
    }

    fun withPreset(id: String) = copy(presetId = AspectPresets.byId(id).id)

    fun withStyle(style: CollageStyle) = copy(style = style)

    fun setPhoto(index: Int, uri: String): CollageState = updateSlot(index) { Slot(uri) }

    fun clearPhoto(index: Int): CollageState = updateSlot(index) { Slot() }

    fun setTransform(index: Int, transform: CellTransform): CollageState =
        if (slot(index).uri == null) this else updateSlot(index) { it.copy(transform = transform) }

    fun resetTransform(index: Int) = setTransform(index, CellTransform.Identity)

    fun swap(a: Int, b: Int): CollageState {
        if (a == b) return this
        val list = padded(maxOf(a, b) + 1)
        val tmp = list[a]
        list[a] = list[b]
        list[b] = tmp
        return copy(slots = list)
    }

    /**
     * Adds photos: fills empty cells first, then grows the layout to the default template for
     * the new photo count (up to [Templates.MAX_PHOTOS]). Extra photos are dropped.
     */
    fun addPhotos(uris: List<String>): CollageState {
        if (uris.isEmpty()) return this
        val list = padded(template.count)
        val queue = ArrayDeque(uris)
        for (i in list.indices) {
            if (queue.isEmpty()) break
            if (list[i].uri == null) list[i] = Slot(queue.removeFirst())
        }
        while (queue.isNotEmpty() && list.count { it.uri != null } < Templates.MAX_PHOTOS) {
            val emptyIndex = list.indexOfFirst { it.uri == null }
            if (emptyIndex >= 0) list[emptyIndex] = Slot(queue.removeFirst()) else list += Slot(queue.removeFirst())
        }
        val filled = list.count { it.uri != null }
        val next = copy(slots = list)
        return if (filled > template.count) next.withTemplate(Templates.defaultFor(filled, preset.aspect).id) else next
    }

    private fun updateSlot(index: Int, f: (Slot) -> Slot): CollageState {
        val list = padded(index + 1)
        list[index] = f(list[index])
        return copy(slots = list)
    }

    private fun padded(size: Int): MutableList<Slot> =
        slots.toMutableList().apply { while (this.size < size) add(Slot()) }

    companion object {
        /**
         * Moves photos into the first [cellCount] slots (preserving order) so that a smaller
         * template doesn't leave its cells empty while photos sit in hidden slots.
         */
        internal fun packed(slots: List<Slot>, cellCount: Int): List<Slot> {
            val visibleEmpty = slots.take(cellCount).count { it.uri == null }
            val hidden = slots.drop(cellCount).filter { it.uri != null }
            if (visibleEmpty == 0 || hidden.isEmpty()) return slots
            val photos = slots.filter { it.uri != null }
            val empties = List(slots.size - photos.size) { Slot() }
            return photos + empties
        }
    }
}
