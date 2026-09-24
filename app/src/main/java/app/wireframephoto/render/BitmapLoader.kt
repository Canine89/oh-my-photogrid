package app.wireframephoto.render

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.util.LruCache
import app.wireframephoto.core.CellTransform
import app.wireframephoto.core.CropMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decodes photos with [ImageDecoder], which applies EXIF orientation and supports JPEG, HEIF and
 * AVIF. Preview bitmaps are cached; export decodes are one-off, cropped, and sized to the cell.
 */
class BitmapLoader(private val resolver: ContentResolver, cacheBytes: Int = DEFAULT_CACHE_BYTES) {

    private val cache = object : LruCache<String, Bitmap>(cacheBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    /** Cached preview whose longer side is at most [maxSide]. Returns null if unreadable. */
    suspend fun loadPreview(uri: String, maxSide: Int = PREVIEW_MAX_SIDE): Bitmap? {
        val key = "$uri@$maxSide"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeBitmap(source(uri)) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val scale = minOf(1f, maxSide.toFloat() / max(w, h))
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }.onFailure { Log.w(TAG, "Preview decode failed for $uri", it) }
                .getOrNull()
                ?.also { cache.put(key, it) }
        }
    }

    fun cachedPreview(uri: String, maxSide: Int = PREVIEW_MAX_SIDE): Bitmap? = cache.get("$uri@$maxSide")

    private val colors = LruCache<String, Int>(64)

    /** Average ARGB color of a photo (from an 8×8 decode), used for the ambient glow behind the canvas. */
    suspend fun averageColor(uri: String): Int? {
        colors.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val tiny = ImageDecoder.decodeBitmap(source(uri)) { decoder, _, _ ->
                    decoder.setTargetSize(8, 8)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                val px = IntArray(tiny.width * tiny.height)
                tiny.getPixels(px, 0, tiny.width, 0, 0, tiny.width, tiny.height)
                tiny.recycle()
                val r = px.sumOf { (it shr 16) and 0xFF } / px.size
                val g = px.sumOf { (it shr 8) and 0xFF } / px.size
                val b = px.sumOf { it and 0xFF } / px.size
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }.getOrNull()?.also { colors.put(uri, it) }
        }
    }

    /**
     * Decodes only the part of [uri] visible in a [cellW]x[cellH] cell under [transform], at about
     * one source pixel per output pixel. Result is a software bitmap safe to draw on any Canvas.
     */
    suspend fun decodeForCell(
        uri: String,
        cellW: Float,
        cellH: Float,
        transform: CellTransform,
        maxPixels: Long = EXPORT_MAX_DECODE_PIXELS,
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            ImageDecoder.decodeBitmap(source(uri)) { decoder, info, _ ->
                val (w, h) = CropMath.decodeSize(info.size.width, info.size.height, cellW, cellH, transform.zoom, maxPixels)
                decoder.setTargetSize(w, h)
                val src = CropMath.sourceRect(w, h, cellW, cellH, transform)
                decoder.crop = Rect(
                    floor(src.left).toInt().coerceIn(0, w - 1),
                    floor(src.top).toInt().coerceIn(0, h - 1),
                    ceil(src.right).toInt().coerceIn(1, w),
                    ceil(src.bottom).toInt().coerceIn(1, h),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.onFailure { Log.w(TAG, "Export decode failed for $uri", it) }.getOrNull()
    }

    fun clear() = cache.evictAll()

    private fun source(uri: String) = ImageDecoder.createSource(resolver, Uri.parse(uri))

    companion object {
        private const val TAG = "BitmapLoader"
        const val PREVIEW_MAX_SIDE = 1440
        const val DEFAULT_CACHE_BYTES = 96 * 1024 * 1024

        /** 32MP ≈ 128MB per decode; photos are decoded one at a time during export. */
        const val EXPORT_MAX_DECODE_PIXELS = 32_000_000L
    }
}
