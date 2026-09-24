package app.wireframephoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

class CollageStateTest {

    private fun uris(n: Int) = (1..n).map { "content://p/$it" }

    @Test
    fun addPhotos_toEmptyState_growsTemplateToPhotoCount() {
        val s = CollageState().addPhotos(uris(4))
        assertEquals(4, s.photoCount)
        assertEquals(4, s.template.count)
        assertEquals("content://p/1", s.slot(0).uri)
    }

    @Test
    fun addPhotos_fillsEmptyCellsFirst() {
        val s = CollageState(templateId = Templates.defaultFor(3).id)
            .addPhotos(uris(3))
            .clearPhoto(1)
            .addPhotos(listOf("content://new"))
        assertEquals("content://new", s.slot(1).uri)
        assertEquals(3, s.template.count)
    }

    @Test
    fun addPhotos_capsAtMaxPhotos() {
        val s = CollageState().addPhotos(uris(12))
        assertEquals(Templates.MAX_PHOTOS, s.photoCount)
        assertEquals(Templates.MAX_PHOTOS, s.template.count)
    }

    @Test
    fun swap_exchangesPhotosAndTransforms() {
        val t = CellTransform(2f, 0.4f, 0.6f)
        val s = CollageState().addPhotos(uris(2)).setTransform(0, t).swap(0, 1)
        assertEquals("content://p/2", s.slot(0).uri)
        assertEquals("content://p/1", s.slot(1).uri)
        assertEquals(t, s.slot(1).transform)
        assertEquals(CellTransform.Identity, s.slot(0).transform)
    }

    @Test
    fun swap_withEmptyCell_movesPhoto() {
        val s = CollageState(templateId = Templates.defaultFor(3).id).addPhotos(uris(1)).swap(0, 2)
        assertNull(s.slot(0).uri)
        assertEquals("content://p/1", s.slot(2).uri)
    }

    @Test
    fun setPhoto_resetsTransform() {
        val s = CollageState().addPhotos(uris(1)).setTransform(0, CellTransform(3f)).setPhoto(0, "content://x")
        assertEquals(CellTransform.Identity, s.slot(0).transform)
    }

    @Test
    fun setTransform_onEmptyCell_isNoOp() {
        val s = CollageState(templateId = Templates.defaultFor(2).id)
        assertSame(s, s.setTransform(1, CellTransform(2f)))
    }

    @Test
    fun smallerTemplate_packsHiddenPhotosIntoVisibleCells() {
        val s = CollageState().addPhotos(uris(4)).clearPhoto(0).withTemplate(Templates.defaultFor(3).id)
        // Cells 0..2 must all show photos since 3 photos remain.
        assertEquals(listOf("content://p/2", "content://p/3", "content://p/4"), (0..2).map { s.slot(it).uri })
    }

    @Test
    fun switchingTemplatesBackAndForth_keepsPhotos() {
        val s = CollageState().addPhotos(uris(4))
        val back = s.withTemplate(Templates.defaultFor(2).id).withTemplate(s.templateId)
        assertEquals(s.slots, back.slots)
    }

    @Test
    fun unknownIds_fallBackSafely() {
        val s = CollageState()
        assertSame(s, s.withTemplate("nope"))
        assertEquals(AspectPresets.Default.id, s.withPreset("nope").presetId)
    }

    @Test
    fun presetSizes_matchFoldScreens() {
        assertEquals(2448 to 1848, AspectPresets.byId("fold8_main").sizeAt(1f))
        assertEquals(1248 to 1972, AspectPresets.byId("fold8_cover").sizeAt(1f))
        assertEquals(624 to 986, AspectPresets.byId("fold8_cover").sizeAt(0.5f))
    }
}

class ShuffleTest {
    private val base = CollageState().addPhotos((1..4).map { "content://p/$it" })

    @Test
    fun shuffle_changesLayoutAndBackground_keepsPhotos() {
        repeat(20) { seed ->
            val s = base.shuffled(Random(seed))
            assertNotEquals(base.templateId, s.templateId)
            assertEquals(base.template.count, s.template.count)
            assertNotEquals(base.style.backgroundColor, s.style.backgroundColor)
            assertEquals(base.slots.map { it.uri }, s.slots.map { it.uri })
        }
    }

    @Test
    fun shuffle_singleLayoutCount_stillValid() {
        val one = CollageState().addPhotos(listOf("content://p/1"))
        val s = one.shuffled(Random(1))
        assertEquals(1, s.template.count)
    }
}
