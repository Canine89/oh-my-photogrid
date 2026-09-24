package app.wireframephoto.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.min

/** Rectangle in normalized template space, where the whole canvas is (0,0)-(1,1). */
data class NRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

/** Rectangle in pixel space. */
data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
}

/**
 * Per-cell photo framing. The photo always covers the cell (center-crop), [zoom] >= 1 magnifies it
 * further, and ([centerX], [centerY]) is the visible center in normalized image coordinates.
 * Being resolution-independent, the same transform frames the preview and the export identically.
 */
@Parcelize
data class CellTransform(
    val zoom: Float = 1f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
) : Parcelable {
    companion object {
        const val MAX_ZOOM = 8f
        val Identity = CellTransform()
    }
}

/** Collage styling, in per-mille (1/1000) of the canvas's shorter side so it scales with output size. */
@Parcelize
data class CollageStyle(
    val spacing: Float = 12f,
    val margin: Float = 12f,
    val cornerRadius: Float = 0f,
    val backgroundColor: Long = 0xFFFFFFFF,
) : Parcelable {
    fun spacingPx(canvasW: Float, canvasH: Float) = spacing / 1000f * min(canvasW, canvasH)
    fun marginPx(canvasW: Float, canvasH: Float) = margin / 1000f * min(canvasW, canvasH)
    fun cornerRadiusPx(canvasW: Float, canvasH: Float) = cornerRadius / 1000f * min(canvasW, canvasH)

    companion object {
        const val MAX_SPACING = 40f
        const val MAX_MARGIN = 60f
        const val MAX_CORNER = 60f
    }
}

object CollageGeometry {
    private const val EPS = 1e-4f

    /**
     * Maps normalized template cells to pixel rects. Outer edges sit on the margin; interior edges
     * are inset by half the spacing on each side so every gap is exactly [spacingPx] wide.
     */
    fun cellRects(
        cells: List<NRect>,
        canvasW: Float,
        canvasH: Float,
        spacingPx: Float,
        marginPx: Float,
    ): List<PxRect> {
        val innerW = canvasW - 2 * marginPx
        val innerH = canvasH - 2 * marginPx
        val half = spacingPx / 2f
        return cells.map { c ->
            PxRect(
                left = marginPx + c.left * innerW + if (c.left > EPS) half else 0f,
                top = marginPx + c.top * innerH + if (c.top > EPS) half else 0f,
                right = marginPx + c.right * innerW - if (c.right < 1f - EPS) half else 0f,
                bottom = marginPx + c.bottom * innerH - if (c.bottom < 1f - EPS) half else 0f,
            )
        }
    }

    fun cellRects(template: Template, canvasW: Float, canvasH: Float, style: CollageStyle) =
        cellRects(
            template.cells, canvasW, canvasH,
            style.spacingPx(canvasW, canvasH), style.marginPx(canvasW, canvasH),
        )

    /** Largest rect of aspect [aspect] (w/h) that fits in [boxW]x[boxH], centered. */
    fun fit(aspect: Float, boxW: Float, boxH: Float): PxRect {
        val w: Float
        val h: Float
        if (boxW / boxH > aspect) {
            h = boxH; w = boxH * aspect
        } else {
            w = boxW; h = boxW / aspect
        }
        val l = (boxW - w) / 2f
        val t = (boxH - h) / 2f
        return PxRect(l, t, l + w, t + h)
    }
}
