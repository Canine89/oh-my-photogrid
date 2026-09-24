package app.wireframephoto.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.wireframephoto.core.CellTransform
import app.wireframephoto.core.CollageGeometry
import app.wireframephoto.core.CropMath
import app.wireframephoto.core.Template
import app.wireframephoto.ui.theme.Wf
import kotlin.math.roundToInt

/** Wireframe drawing of a template at the given canvas aspect ratio. */
@Composable
fun TemplateThumbnail(
    template: Template,
    aspect: Float,
    cellColors: List<Color>,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
) {
    Canvas(modifier.aspectRatio(aspect)) {
        val gap = 2.5.dp.toPx()
        drawRoundRect(background, cornerRadius = CornerRadius(3.dp.toPx()))
        val rects = CollageGeometry.cellRects(template.cells, size.width, size.height, gap, gap)
        rects.forEachIndexed { i, r ->
            drawRoundRect(
                color = cellColors[i % cellColors.size],
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/**
 * Layout preview filled with the user's own photos (center-cropped, in slot order), so choosing
 * a layout is choosing how *these* pictures will look — not an abstract grid.
 */
@Composable
fun LiveTemplateThumbnail(
    template: Template,
    aspect: Float,
    photos: List<ImageBitmap?>,
    modifier: Modifier = Modifier,
) {
    val print = Wf.Print
    val hole = Wf.PrintHole
    Canvas(modifier.aspectRatio(aspect)) {
        val gap = 2.dp.toPx()
        val corner = CornerRadius(3.dp.toPx())
        drawRoundRect(print.copy(alpha = 0.9f), cornerRadius = CornerRadius(5.dp.toPx()))
        CollageGeometry.cellRects(template.cells, size.width, size.height, gap, gap).forEachIndexed { i, r ->
            val img = photos.getOrNull(i)
            val path = Path().apply { addRoundRect(RoundRect(r.left, r.top, r.right, r.bottom, corner)) }
            clipPath(path) {
                if (img != null) {
                    val src = CropMath.sourceRect(img.width, img.height, r.width, r.height, CellTransform.Identity)
                    val sx = src.left.roundToInt().coerceIn(0, img.width - 1)
                    val sy = src.top.roundToInt().coerceIn(0, img.height - 1)
                    drawImage(
                        img,
                        srcOffset = IntOffset(sx, sy),
                        srcSize = IntSize(
                            src.width.roundToInt().coerceIn(1, img.width - sx),
                            src.height.roundToInt().coerceIn(1, img.height - sy),
                        ),
                        dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()),
                        dstSize = IntSize(r.width.roundToInt().coerceAtLeast(1), r.height.roundToInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.Low,
                    )
                } else {
                    drawRect(hole, Offset(r.left, r.top), Size(r.width, r.height))
                }
            }
        }
    }
}
