package app.wireframephoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatesTest {

    @Test
    fun idsAreUnique() {
        val ids = Templates.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun everyPhotoCountHasTemplates() {
        for (n in 1..Templates.MAX_PHOTOS) {
            assertTrue("no template for $n", Templates.forCount(n).isNotEmpty())
            assertEquals(n, Templates.defaultFor(n).count)
        }
        assertTrue(Templates.all.size >= 30)
    }

    @Test
    fun cellsStayInsideUnitSquare() {
        for (t in Templates.all) for (c in t.cells) {
            assertTrue("${t.id} $c", c.left >= 0f && c.top >= 0f && c.right <= 1f && c.bottom <= 1f)
            assertTrue("${t.id} $c degenerate", c.width > 0.05f && c.height > 0.05f)
        }
    }

    @Test
    fun cellsTileTheCanvasWithoutOverlap() {
        for (t in Templates.all) {
            val area = t.cells.sumOf { (it.width * it.height).toDouble() }
            assertEquals("${t.id} area", 1.0, area, 1e-4)
            for (i in t.cells.indices) for (j in i + 1 until t.cells.size) {
                val a = t.cells[i]
                val b = t.cells[j]
                val ox = minOf(a.right, b.right) - maxOf(a.left, b.left)
                val oy = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
                assertFalse("${t.id} cells $i and $j overlap", ox > 1e-4f && oy > 1e-4f)
            }
        }
    }

    @Test
    fun lookupById() {
        for (t in Templates.all) assertNotNull(Templates.byId(t.id))
    }

    @Test
    fun defaultFor_prefersNearSquareCellsForCanvasShape() {
        val cover = AspectPresets.byId("fold8_cover").aspect
        val main = AspectPresets.byId("fold8_main").aspect
        // Tall cover canvas: 6 photos → 2 columns × 3 rows, not 3 skinny columns.
        assertEquals("rows_2-2-2", Templates.defaultFor(6, cover).id)
        // Wide main canvas: 6 photos → 3 columns × 2 rows.
        assertEquals("rows_3-3", Templates.defaultFor(6, main).id)
        assertEquals("rows_2-2", Templates.defaultFor(4, main).id)
    }
}
