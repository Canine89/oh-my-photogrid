package app.wireframephoto.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.wireframephoto.core.CollageGeometry
import app.wireframephoto.core.CollageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Renders a [CollageState] off-screen at full export resolution. */
class CollageRenderer(private val loader: BitmapLoader) {

    suspend fun render(
        state: CollageState,
        width: Int,
        height: Int,
        onProgress: (Float) -> Unit = {},
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = width.toFloat()
        val h = height.toFloat()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(state.style.backgroundColor.toInt())

        val rects = CollageGeometry.cellRects(state.template, w, h, state.style)
        val radius = state.style.cornerRadiusPx(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        rects.forEachIndexed { i, r ->
            ensureActive()
            val slot = state.slot(i)
            val uri = slot.uri
            if (uri != null && r.width >= 1f && r.height >= 1f) {
                val photo = loader.decodeForCell(uri, r.width, r.height, slot.transform)
                if (photo != null) {
                    val dst = RectF(r.left, r.top, r.right, r.bottom)
                    canvas.save()
                    if (radius > 0f) {
                        canvas.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
                    } else {
                        canvas.clipRect(dst)
                    }
                    canvas.drawBitmap(photo, null, dst, paint)
                    canvas.restore()
                    photo.recycle()
                }
            }
            onProgress((i + 1f) / rects.size)
        }
        output
    }
}
