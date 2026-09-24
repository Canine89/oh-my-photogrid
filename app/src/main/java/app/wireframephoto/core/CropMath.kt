package app.wireframephoto.core

import kotlin.math.max

/**
 * Framing math shared by the on-screen preview and the full-resolution export.
 * All functions depend only on aspect ratios and normalized coordinates, so a transform produced
 * by gestures on a small preview crops the exact same region from a large export.
 */
object CropMath {

    /** Fraction of the image width/height visible in a cell of [cellW]x[cellH]. */
    fun visibleFraction(imageAspect: Float, cellW: Float, cellH: Float, zoom: Float): Pair<Float, Float> {
        // A degenerate (e.g. mid-animation, zero-size) cell shows the whole image rather than NaN.
        if (cellW <= 0f || cellH <= 0f) return 1f to 1f
        // Size of the image drawn with center-crop "cover" scaling, times zoom.
        val drawnW = zoom * max(cellW, cellH * imageAspect)
        val drawnH = zoom * max(cellW / imageAspect, cellH)
        return (cellW / drawnW) to (cellH / drawnH)
    }

    /** Clamps zoom to [1, MAX_ZOOM] and the center so the visible window stays inside the image. */
    fun clamp(t: CellTransform, imageAspect: Float, cellW: Float, cellH: Float): CellTransform {
        val zoom = t.zoom.coerceIn(1f, CellTransform.MAX_ZOOM)
        val (fw, fh) = visibleFraction(imageAspect, cellW, cellH, zoom)
        return CellTransform(
            zoom = zoom,
            centerX = t.centerX.coerceIn(fw / 2f, 1f - fw / 2f),
            centerY = t.centerY.coerceIn(fh / 2f, 1f - fh / 2f),
        )
    }

    /**
     * Source rectangle (in bitmap pixels) to draw into a cell. [bitmapW]x[bitmapH] may be any
     * downsampled version of the original as long as the aspect ratio is preserved.
     */
    fun sourceRect(
        bitmapW: Int,
        bitmapH: Int,
        cellW: Float,
        cellH: Float,
        transform: CellTransform,
    ): PxRect {
        val aspect = bitmapW.toFloat() / bitmapH
        val t = clamp(transform, aspect, cellW, cellH)
        val (fw, fh) = visibleFraction(aspect, cellW, cellH, t.zoom)
        val left = (t.centerX - fw / 2f) * bitmapW
        val top = (t.centerY - fh / 2f) * bitmapH
        return PxRect(left, top, left + fw * bitmapW, top + fh * bitmapH)
    }

    /**
     * Applies a gesture (pan in pixels, zoom factor around [focusX],[focusY] relative to the cell's
     * top-left) to a transform, keeping the image point under the focus fixed.
     */
    fun applyGesture(
        t: CellTransform,
        imageAspect: Float,
        cellW: Float,
        cellH: Float,
        panX: Float,
        panY: Float,
        zoomChange: Float,
        focusX: Float,
        focusY: Float,
    ): CellTransform {
        val oldDrawnW = t.zoom * max(cellW, cellH * imageAspect)
        val oldDrawnH = t.zoom * max(cellW / imageAspect, cellH)
        val newZoom = (t.zoom * zoomChange).coerceIn(1f, CellTransform.MAX_ZOOM)
        val newDrawnW = newZoom * max(cellW, cellH * imageAspect)
        val newDrawnH = newZoom * max(cellW / imageAspect, cellH)

        val dx = focusX - cellW / 2f
        val dy = focusY - cellH / 2f
        // Image point (normalized) currently under the focus.
        val px = t.centerX + dx / oldDrawnW
        val py = t.centerY + dy / oldDrawnH
        // After zooming and panning, the same image point should sit under focus + pan.
        val cx = px - (dx + panX) / newDrawnW
        val cy = py - (dy + panY) / newDrawnH
        return clamp(CellTransform(newZoom, cx, cy), imageAspect, cellW, cellH)
    }

    /**
     * Decode width/height that renders the visible part of an image at roughly one source pixel per
     * output pixel, never upscaling beyond the original and never exceeding [maxPixels].
     */
    fun decodeSize(
        originalW: Int,
        originalH: Int,
        cellW: Float,
        cellH: Float,
        zoom: Float,
        maxPixels: Long,
    ): Pair<Int, Int> {
        val aspect = originalW.toFloat() / originalH
        val z = zoom.coerceIn(1f, CellTransform.MAX_ZOOM)
        var w = z * max(cellW, cellH * aspect)
        var h = w / aspect
        if (w > originalW) {
            w = originalW.toFloat(); h = originalH.toFloat()
        }
        val pixels = w.toDouble() * h
        if (pixels > maxPixels) {
            val s = kotlin.math.sqrt(maxPixels / pixels).toFloat()
            w *= s; h *= s
        }
        return w.toInt().coerceAtLeast(1) to h.toInt().coerceAtLeast(1)
    }
}
