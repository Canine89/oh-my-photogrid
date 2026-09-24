package app.wireframephoto.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val mimeType: String, val extension: String, val compress: Bitmap.CompressFormat, val quality: Int) {
    JPEG("image/jpeg", "jpg", Bitmap.CompressFormat.JPEG, 95),
    PNG("image/png", "png", Bitmap.CompressFormat.PNG, 100),
}

object ImageSaver {
    const val ALBUM = "oh-my-photogrid"

    /** Writes [bitmap] to Pictures/oh-my-photogrid via MediaStore (no permission needed on API 29+). */
    suspend fun save(context: Context, bitmap: Bitmap, format: ExportFormat, date: Date = Date()): Uri =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = "Collage_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date) + "." + format.extension
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM)
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(format.compress, format.quality, out)) throw IOException("Encoding failed")
                } ?: throw IOException("Cannot open $uri")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

    fun shareIntent(uri: Uri, mimeType: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        null,
    )

    /** "Set as" chooser; on Galaxy devices this offers home/lock screen wallpaper via Gallery. */
    fun setAsIntent(uri: Uri, mimeType: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(uri, mimeType)
            putExtra("mimeType", mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        null,
    )
}
