package app.wireframephoto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.wireframephoto.core.CellTransform
import app.wireframephoto.core.CollageState
import app.wireframephoto.core.CollageStyle
import app.wireframephoto.core.Slot
import app.wireframephoto.core.Templates
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.render.CollageRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class RenderInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val loader = BitmapLoader(context.contentResolver)
    private val renderer = CollageRenderer(loader)

    private fun writePng(name: String, w: Int, h: Int, draw: (Canvas) -> Unit): Uri {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        val f = File(context.cacheDir, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(f)
    }

    private fun solid(name: String, w: Int, h: Int, color: Int) = writePng(name, w, h) { it.drawColor(color) }

    private fun assertColor(expected: Int, actual: Int, tolerance: Int = 12) {
        val ok = abs(Color.red(expected) - Color.red(actual)) <= tolerance &&
            abs(Color.green(expected) - Color.green(actual)) <= tolerance &&
            abs(Color.blue(expected) - Color.blue(actual)) <= tolerance
        assertTrue("expected #%08X but was #%08X".format(expected, actual), ok)
    }

    @Test
    fun render_placesPhotosInCells_withExactOutputSize() = runBlocking {
        val red = solid("red.png", 400, 300, Color.RED)
        val blue = solid("blue.png", 300, 400, Color.BLUE)
        val state = CollageState(
            slots = listOf(Slot(red.toString()), Slot(blue.toString())),
            templateId = Templates.cols(1, 1).id,
            style = CollageStyle(spacing = 0f, margin = 50f, backgroundColor = 0xFF00FF00),
        )
        val out = renderer.render(state, 1000, 500)
        assertEquals(1000, out.width)
        assertEquals(500, out.height)
        // margin = 50‰ of 500 = 25px of background.
        assertColor(Color.GREEN, out.getPixel(5, 5))
        assertColor(Color.RED, out.getPixel(250, 250))
        assertColor(Color.BLUE, out.getPixel(750, 250))
        out.recycle()
    }

    @Test
    fun render_appliesCellTransform() = runBlocking {
        // Left half red, right half blue.
        val split = writePng("split.png", 800, 400) {
            it.drawColor(Color.BLUE)
            it.drawRect(0f, 0f, 400f, 400f, Paint().apply { color = Color.RED })
        }
        val base = CollageState(
            slots = listOf(Slot(split.toString())),
            templateId = Templates.defaultFor(1).id,
            style = CollageStyle(spacing = 0f, margin = 0f),
        )
        // Square cell shows the center half of the image: left edge red, right edge blue.
        val centered = renderer.render(base, 400, 400)
        assertColor(Color.RED, centered.getPixel(20, 200))
        assertColor(Color.BLUE, centered.getPixel(380, 200))
        centered.recycle()

        // Panned fully left: the whole cell shows the red half.
        val panned = renderer.render(base.setTransform(0, CellTransform(1f, 0f, 0.5f)), 400, 400)
        assertColor(Color.RED, panned.getPixel(20, 200))
        assertColor(Color.RED, panned.getPixel(380, 200))
        panned.recycle()
    }

    @Test
    fun render_roundedCornersShowBackground() = runBlocking {
        val red = solid("red2.png", 200, 200, Color.RED)
        val state = CollageState(
            slots = listOf(Slot(red.toString())),
            templateId = Templates.defaultFor(1).id,
            style = CollageStyle(spacing = 0f, margin = 0f, cornerRadius = 60f, backgroundColor = 0xFFFFFFFF),
        )
        val out = renderer.render(state, 1000, 1000)
        assertColor(Color.WHITE, out.getPixel(1, 1))
        assertColor(Color.RED, out.getPixel(500, 500))
        out.recycle()
    }

    @Test
    fun exifOrientation_isApplied() = runBlocking {
        val f = File(context.cacheDir, "exif_rotated.jpg")
        testContext.assets.open("exif_rotated.jpg").use { input -> f.outputStream().use { input.copyTo(it) } }
        val preview = loader.loadPreview(Uri.fromFile(f).toString())
        assertNotNull(preview)
        preview!!
        // Stored as 400x300 landscape; EXIF orientation 6 makes it 300x400 portrait, red on top.
        assertEquals(300, preview.width)
        assertEquals(400, preview.height)
        val soft = preview.copy(Bitmap.Config.ARGB_8888, false)
        assertColor(Color.RED, soft.getPixel(150, 50), tolerance = 30)
        assertColor(Color.BLUE, soft.getPixel(150, 350), tolerance = 30)
    }

    @Test
    fun largeSource_isDownsampledForExport() = runBlocking {
        // A 50MP-class photo into a small cell must not be decoded at full size.
        val big = solid("big.png", 8160, 6120, Color.MAGENTA)
        val bmp = loader.decodeForCell(big.toString(), 300f, 300f, CellTransform.Identity)
        assertNotNull(bmp)
        assertTrue("decoded ${bmp!!.width}x${bmp.height}", bmp.width <= 310 && bmp.height <= 310)
        bmp.recycle()
    }

    @Test
    fun unreadableUri_rendersBackgroundInsteadOfCrashing() = runBlocking {
        val state = CollageState(
            slots = listOf(Slot("file:///does/not/exist.jpg")),
            templateId = Templates.defaultFor(1).id,
            style = CollageStyle(backgroundColor = 0xFF123456),
        )
        val out = renderer.render(state, 100, 100)
        assertColor(0xFF123456.toInt(), out.getPixel(50, 50))
        out.recycle()
    }
}
