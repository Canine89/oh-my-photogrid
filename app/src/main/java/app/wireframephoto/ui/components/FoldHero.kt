package app.wireframephoto.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.wireframephoto.core.AspectPresets
import app.wireframephoto.core.CollageGeometry
import app.wireframephoto.core.Templates
import app.wireframephoto.ui.theme.LocalReducedMotion
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf
import kotlinx.coroutines.delay

/** Photo-like gradient "scenes" so the hero reads as pictures on a screen, not as a grid to pick. */
private val Scenes = listOf(
    listOf(Color(0xFFFF9A6B), Color(0xFF8B5CF6)), // sunset
    listOf(Color(0xFF7DD3FC), Color(0xFF1E3A8A)), // sea
    listOf(Color(0xFFD4FF4F), Color(0xFF166534)), // forest
    listOf(Color(0xFFFDE68A), Color(0xFFEA580C)), // desert
    listOf(Color(0xFFC4B5FD), Color(0xFF1E1B4B)), // dusk
)

/**
 * Home hero: a Galaxy Z Fold 8 that keeps folding (cover 10:16) and unfolding (main 4:3) while the
 * photos inside re-tile to fit — the whole promise of the app in one loop.
 */
@Composable
fun FoldHero(height: Dp, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    var unfolded by remember { mutableStateOf(true) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        while (true) {
            delay(2600)
            unfolded = !unfolded
        }
    }
    val coverAspect = AspectPresets.byId("fold8_cover").aspect
    val mainAspect = AspectPresets.byId("fold8_main").aspect
    val aspect by animateFloatAsState(if (unfolded) mainAspect else coverAspect, Wf.bouncy(), label = "aspect")
    val template = if (unfolded) Templates.defaultFor(4, mainAspect) else Templates.defaultFor(3, coverAspect)
    val cells = animateCells(template.cells.map { Rect(it.left, it.top, it.right, it.bottom) })

    val glow = Wf.GlowBrush
    val body = Wf.DeviceBody
    val edge = Wf.DeviceEdge
    val screenColor = Wf.DeviceScreen
    val palette = LocalWfPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            drawRect(glow, size = size)
            val bezel = 6.dp.toPx()
            val box = CollageGeometry.fit(aspect, size.width * 0.9f, size.height * 0.92f)
            val screen = Rect(
                (size.width - box.width) / 2f + bezel, (size.height - box.height) / 2f + bezel,
                (size.width + box.width) / 2f - bezel, (size.height + box.height) / 2f - bezel,
            )
            val caseTopLeft = Offset(screen.left - bezel, screen.top - bezel)
            val caseSize = Size(screen.width + 2 * bezel, screen.height + 2 * bezel)
            val caseCorner = CornerRadius(18.dp.toPx())
            drawRoundRect(body, caseTopLeft, caseSize, caseCorner)
            // Album: a frosted glass phone, its rim catching the light at the top.
            val rim = if (palette.glass) {
                Brush.verticalGradient(listOf(edge, edge.copy(alpha = 0.3f)), caseTopLeft.y, caseTopLeft.y + caseSize.height)
            } else {
                SolidColor(edge)
            }
            drawRoundRect(rim, caseTopLeft, caseSize, caseCorner, style = Stroke((if (palette.styled) 1.5.dp else 1.dp).toPx()))
            drawRoundRect(screenColor, Offset(screen.left, screen.top), Size(screen.width, screen.height), CornerRadius(12.dp.toPx()))
            val gap = 3.dp.toPx()
            val px = CollageGeometry.cellRects(
                cells.map { app.wireframephoto.core.NRect(it.left, it.top, it.right, it.bottom) },
                screen.width, screen.height, gap, gap,
            )
            px.forEachIndexed { i, r ->
                if (r.width > 2f && r.height > 2f) {
                    val cell = Rect(screen.left + r.left, screen.top + r.top, screen.left + r.right, screen.top + r.bottom)
                    // Blueprint: the photos themselves are drawn as wireframes.
                    if (palette.blueprint) drawWireScene(cell) else drawScene(cell, Scenes[i % Scenes.size])
                }
            }
            drawCircle(Color.Black, 3.dp.toPx(), Offset(screen.center.x, screen.top + 8.dp.toPx()))
        }
        AnimatedContent(unfolded, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "caption") { open ->
            Text(
                if (open) "펼쳤을 때 · 메인 화면 4:3" else "접었을 때 · 커버 화면 10:16",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun DrawScope.drawScene(r: Rect, colors: List<Color>) {
    val corner = CornerRadius(8.dp.toPx())
    drawRoundRect(Brush.verticalGradient(colors, r.top, r.bottom), r.topLeft, r.size, corner)
    // Mountain silhouette + sun: enough to say "photo" without pretending to be one.
    clipRect(r.left, r.top, r.right, r.bottom) {
        val h = r.height
        val w = r.width
        drawCircle(Color.White.copy(alpha = 0.55f), minOf(w, h) * 0.08f, Offset(r.left + w * 0.72f, r.top + h * 0.3f))
        drawPath(
            Path().apply {
                moveTo(r.left, r.bottom)
                lineTo(r.left, r.top + h * 0.72f)
                lineTo(r.left + w * 0.32f, r.top + h * 0.5f)
                lineTo(r.left + w * 0.55f, r.top + h * 0.7f)
                lineTo(r.left + w * 0.75f, r.top + h * 0.58f)
                lineTo(r.right, r.top + h * 0.75f)
                lineTo(r.right, r.bottom)
                close()
            },
            Color.Black.copy(alpha = 0.28f),
        )
    }
}
