package app.wireframephoto.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.wireframephoto.ui.theme.LocalWfPalette

/*
 * "청사진" (cyanotype blueprint): the app drawn as a technical drawing on Prussian blue paper.
 * Surfaces are white outlines rather than fills; large ones carry printer's crop marks at their
 * corners; things you haven't picked are dashed, like a draft; the picked one is solid white.
 */

private val Ink = Color(0xD9F4F8FC)
private val FaintInk = Color(0x73F4F8FC)

/**
 * A surface outlined in white. [fill] makes it solid (selections, primary buttons), [dashed] marks
 * an option not yet chosen, [marks] adds crop marks just outside the corners.
 */
fun Modifier.blueprint(shape: Shape, fill: Color? = null, dashed: Boolean = false, marks: Boolean = false): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = 1.5.dp.toPx()
        val dash = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
        val tick = 8.dp.toPx()
        val gap = 5.dp.toPx()
        onDrawBehind {
            // A faint wash so an outlined surface still reads as a separate sheet of paper.
            drawOutline(outline, fill ?: Color(0x0FFFFFFF))
            // Accent fills are solid white already; paper sheets keep their white outline.
            if (fill == null || fill.luminance() < 0.5f) drawOutline(outline, if (dashed) FaintInk else Ink, style = Stroke(stroke, pathEffect = dash))
            if (marks) drawCropMarks(tick, gap, stroke * 0.8f)
        }
    }

/** Printer's crop marks: two short hairlines off each corner, just outside the sheet. */
private fun DrawScope.drawCropMarks(tick: Float, gap: Float, width: Float) {
    val w = size.width
    val h = size.height
    val c = FaintInk
    fun line(a: Offset, b: Offset) = drawLine(c, a, b, width, StrokeCap.Round)
    // top-left
    line(Offset(-gap - tick, 0f), Offset(-gap, 0f)); line(Offset(0f, -gap - tick), Offset(0f, -gap))
    // top-right
    line(Offset(w + gap, 0f), Offset(w + gap + tick, 0f)); line(Offset(w, -gap - tick), Offset(w, -gap))
    // bottom-left
    line(Offset(-gap - tick, h), Offset(-gap, h)); line(Offset(0f, h + gap), Offset(0f, h + gap + tick))
    // bottom-right
    line(Offset(w + gap, h), Offset(w + gap + tick, h)); line(Offset(w, h + gap), Offset(w, h + gap + tick))
}

/** Prussian-blue drafting paper: a fine grid, a bolder grid every fifth line, a soft vignette. */
@Composable
fun BlueprintPaper(modifier: Modifier = Modifier) {
    val palette = LocalWfPalette.current
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF16477F), palette.bg, Color(0xFF0C2D57))))
        val minor = 24.dp.toPx()
        val px = 1.dp.toPx().coerceAtLeast(1f)
        var i = 0
        var x = 0f
        while (x <= size.width) {
            drawLine(Color.White.copy(alpha = if (i % 5 == 0) 0.11f else 0.045f), Offset(x, 0f), Offset(x, size.height), px)
            x += minor; i++
        }
        i = 0
        var y = 0f
        while (y <= size.height) {
            drawLine(Color.White.copy(alpha = if (i % 5 == 0) 0.11f else 0.045f), Offset(0f, y), Offset(size.width, y), px)
            y += minor; i++
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x40000A1A)), center, size.maxDimension * 0.75f))
    }
}

/** A landscape drawn as a wireframe (hero illustration in the blueprint theme). */
fun DrawScope.drawWireScene(r: Rect, color: Color = Ink) {
    val w = r.width
    val h = r.height
    val stroke = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRect(color.copy(alpha = 0.06f), r.topLeft, r.size)
    drawRect(color, r.topLeft, r.size, style = Stroke(1.dp.toPx()))
    drawCircle(color, minOf(w, h) * 0.08f, Offset(r.left + w * 0.72f, r.top + h * 0.3f), style = stroke)
    drawPath(
        Path().apply {
            moveTo(r.left, r.top + h * 0.72f)
            lineTo(r.left + w * 0.32f, r.top + h * 0.5f)
            lineTo(r.left + w * 0.55f, r.top + h * 0.7f)
            lineTo(r.left + w * 0.75f, r.top + h * 0.58f)
            lineTo(r.right, r.top + h * 0.75f)
        },
        color,
        style = stroke,
    )
}
