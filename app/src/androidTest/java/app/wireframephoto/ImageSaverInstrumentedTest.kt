package app.wireframephoto

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.wireframephoto.data.ExportFormat
import app.wireframephoto.data.ImageSaver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageSaverInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun save_writesToPicturesAlbum_withoutPermissions() = runBlocking {
        for (format in ExportFormat.entries) {
            val bmp = Bitmap.createBitmap(123, 45, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF336699.toInt()) }
            val uri = ImageSaver.save(context, bmp, format)
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.IS_PENDING),
                    null, null, null,
                )!!.use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("Pictures/${ImageSaver.ALBUM}/", c.getString(0))
                    assertEquals(format.mimeType, c.getString(1))
                    assertEquals(0, c.getInt(2))
                }
                val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                assertEquals(123, decoded.width)
                assertEquals(45, decoded.height)
            } finally {
                context.contentResolver.delete(uri, null, null)
            }
        }
    }
}
