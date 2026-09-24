package app.wireframephoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {

    private val eps = 0.5f

    @Test
    fun identity_landscapeImageInSquareCell_cropsCenterSquare() {
        val src = CropMath.sourceRect(4000, 3000, 500f, 500f, CellTransform.Identity)
        assertEquals(500f, src.left, eps)
        assertEquals(0f, src.top, eps)
        assertEquals(3000f, src.width, eps)
        assertEquals(3000f, src.height, eps)
    }

    @Test
    fun identity_portraitImageInWideCell_cropsCenterBand() {
        val src = CropMath.sourceRect(3000, 4000, 800f, 400f, CellTransform.Identity)
        assertEquals(0f, src.left, eps)
        assertEquals(3000f, src.width, eps)
        assertEquals(1500f, src.height, eps)
        assertEquals(1250f, src.top, eps)
    }

    @Test
    fun zoom2_halvesVisibleRegion() {
        val src = CropMath.sourceRect(4000, 3000, 500f, 500f, CellTransform(zoom = 2f))
        assertEquals(1500f, src.width, eps)
        assertEquals(1500f, src.height, eps)
        assertEquals(2000f, src.centerX, eps)
        assertEquals(1500f, src.centerY, eps)
    }

    @Test
    fun sourceRect_isResolutionIndependent() {
        val t = CellTransform(zoom = 2.5f, centerX = 0.3f, centerY = 0.6f)
        val full = CropMath.sourceRect(8000, 6000, 1200f, 900f, t)
        val preview = CropMath.sourceRect(1440, 1080, 300f, 225f, t)
        val k = 8000f / 1440f
        assertEquals(full.left, preview.left * k, 2f)
        assertEquals(full.top, preview.top * k, 2f)
        assertEquals(full.width, preview.width * k, 2f)
    }

    @Test
    fun clamp_keepsWindowInsideImage() {
        val t = CropMath.clamp(CellTransform(zoom = 20f, centerX = -1f, centerY = 5f), 4f / 3f, 500f, 500f)
        assertEquals(CellTransform.MAX_ZOOM, t.zoom, 1e-4f)
        val src = CropMath.sourceRect(4000, 3000, 500f, 500f, t)
        assertTrue(src.left >= -0.01f && src.top >= -0.01f)
        assertTrue(src.right <= 4000.01f && src.bottom <= 3000.01f)
    }

    @Test
    fun clamp_atZoom1_cannotPanAlongFittedAxis() {
        // Landscape image in a square cell: full height visible, so vertical pan is locked to center.
        val t = CropMath.clamp(CellTransform(1f, 0.1f, 0.9f), 4f / 3f, 500f, 500f)
        assertEquals(0.5f, t.centerY, 1e-4f)
        assertEquals(0.375f, t.centerX, 1e-4f) // visible fraction 0.75 → min center 0.375
    }

    @Test
    fun pan_movesImageWithFinger() {
        // Zoomed so both axes can pan. Dragging right should reveal content to the left (centerX decreases).
        val start = CellTransform(zoom = 2f)
        val moved = CropMath.applyGesture(start, 1f, 400f, 400f, panX = 100f, panY = 0f, zoomChange = 1f, focusX = 200f, focusY = 200f)
        // Drawn width = 2 * 400 = 800px → 100px pan = 0.125 normalized.
        assertEquals(0.375f, moved.centerX, 1e-4f)
        assertEquals(0.5f, moved.centerY, 1e-4f)
    }

    @Test
    fun pinch_keepsFocusPointFixed() {
        val start = CellTransform(zoom = 1f)
        val cellW = 400f
        val cellH = 400f
        val focusX = 300f
        val focusY = 100f
        val t = CropMath.applyGesture(start, 1f, cellW, cellH, 0f, 0f, zoomChange = 2f, focusX = focusX, focusY = focusY)
        // Image point under focus before: 0.5 + (300-200)/400 = 0.75; after: centerX + 100/800 must equal 0.75.
        assertEquals(0.75f, t.centerX + (focusX - cellW / 2) / (2f * cellW), 1e-4f)
        assertEquals(0.25f, t.centerY + (focusY - cellH / 2) / (2f * cellH), 1e-4f)
        assertEquals(2f, t.zoom, 1e-4f)
    }

    @Test
    fun decodeSize_neverUpscales_andRespectsPixelBudget() {
        val (w, h) = CropMath.decodeSize(1000, 750, 3000f, 3000f, 1f, 100_000_000L)
        assertEquals(1000, w)
        assertEquals(750, h)

        // 200MP-class source, big zoom: must stay under the pixel budget.
        val (bw, bh) = CropMath.decodeSize(16320, 12240, 1500f, 1500f, 8f, 32_000_000L)
        assertTrue(bw.toLong() * bh <= 32_000_000L)
        assertEquals(16320f / 12240f, bw.toFloat() / bh, 0.01f)
    }

    @Test
    fun decodeSize_matchesCellResolution() {
        // 4:3 image into a 1000x1000 cell → cover scaling needs height 1000, width 1333.
        val (w, h) = CropMath.decodeSize(8000, 6000, 1000f, 1000f, 1f, 100_000_000L)
        assertEquals(1333, w)
        assertEquals(1000, h, 1)
    }

    private fun assertEquals(expected: Int, actual: Int, delta: Int) =
        assertTrue("expected $expected±$delta but was $actual", kotlin.math.abs(expected - actual) <= delta)

    @Test
    fun zeroSizeCell_neverProducesNaN() {
        // Cells animate in from zero size; the preview must not crash on the first frame.
        for ((w, h) in listOf(0f to 0f, 0f to 100f, 100f to 0f)) {
            val src = CropMath.sourceRect(4000, 3000, w, h, CellTransform(2f, 0.3f, 0.7f))
            assertTrue("$w x $h -> $src", listOf(src.left, src.top, src.right, src.bottom).none { it.isNaN() })
        }
    }
}
