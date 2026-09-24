package app.wireframephoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {

    private val eps = 0.01f

    @Test
    fun singleCell_fillsCanvasInsideMargin() {
        val rects = CollageGeometry.cellRects(listOf(NRect(0f, 0f, 1f, 1f)), 1000f, 800f, 20f, 10f)
        assertEquals(PxRect(10f, 10f, 990f, 790f), rects.single())
    }

    @Test
    fun twoColumns_gapEqualsSpacing_andOuterEdgesOnMargin() {
        val t = Templates.cols(1, 1)
        val rects = CollageGeometry.cellRects(t.cells, 1000f, 500f, 30f, 12f)
        val (a, b) = rects
        assertEquals(12f, a.left, eps)
        assertEquals(988f, b.right, eps)
        assertEquals(30f, b.left - a.right, eps)
        assertEquals(a.width, b.width, eps)
        assertEquals(12f, a.top, eps)
        assertEquals(488f, a.bottom, eps)
    }

    @Test
    fun everyTemplate_allGapsEqualSpacing() {
        val spacing = 24f
        for (t in Templates.all) {
            val rects = CollageGeometry.cellRects(t.cells, 2448f, 1848f, spacing, 0f)
            // Any two cells that share an interior edge in template space must be exactly `spacing` apart.
            for (i in t.cells.indices) for (j in t.cells.indices) {
                if (i == j) continue
                val ci = t.cells[i]
                val cj = t.cells[j]
                val overlapY = minOf(ci.bottom, cj.bottom) - maxOf(ci.top, cj.top) > 1e-4f
                val overlapX = minOf(ci.right, cj.right) - maxOf(ci.left, cj.left) > 1e-4f
                if (overlapY && kotlin.math.abs(ci.right - cj.left) < 1e-4f) {
                    assertEquals("${t.id} horizontal gap $i|$j", spacing, rects[j].left - rects[i].right, eps)
                }
                if (overlapX && kotlin.math.abs(ci.bottom - cj.top) < 1e-4f) {
                    assertEquals("${t.id} vertical gap $i|$j", spacing, rects[j].top - rects[i].bottom, eps)
                }
            }
        }
    }

    @Test
    fun spacingScalesWithCanvas_soPreviewAndExportMatchProportionally() {
        val style = CollageStyle(spacing = 20f, margin = 10f)
        val t = Templates.defaultFor(4)
        val small = CollageGeometry.cellRects(t, 612f, 462f, style)
        val big = CollageGeometry.cellRects(t, 2448f, 1848f, style)
        small.zip(big).forEach { (s, b) ->
            assertEquals(s.width / s.height, b.width / b.height, 1e-3f)
            assertEquals(b.left / 4f, s.left, 0.01f)
        }
    }

    @Test
    fun fit_preservesAspectAndCenters() {
        val r = CollageGeometry.fit(4f / 3f, 1000f, 1000f)
        assertEquals(1000f, r.width, eps)
        assertEquals(750f, r.height, eps)
        assertEquals(125f, r.top, eps)
        val tall = CollageGeometry.fit(1248f / 1972f, 1000f, 1000f)
        assertEquals(1000f, tall.height, eps)
        assertTrue(tall.left > 0f)
    }
}
