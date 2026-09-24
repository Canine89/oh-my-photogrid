package app.wireframephoto.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.wireframephoto.ui.theme.LocalReducedMotion
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * "Jelly" material for the 가족 앨범 theme: surfaces look like soft gummy candy you could press
 * a thumb into, and move like it. Recipe (claymorphism / 2026 "digital texture"):
 * - a soft drop shadow tinted with the surface's own hue, never gray
 * - a body that is lighter on top and a little deeper at the bottom (a rounded, filled volume)
 * - a glossy highlight across the top and a bright rim, a darker rim pooling at the bottom
 * - squash-and-stretch on press, a wobbly overshoot on release
 * In Darkroom every helper here falls back to the plain, flat behavior.
 */

/** Jelly (Album theme) or flat fill (Darkroom) for a surface of [color] in [shape]. */
@Composable
fun Modifier.wfSurface(color: Color, shape: Shape, depth: Dp = 8.dp): Modifier {
    if (!LocalWfPalette.current.jelly) return background(color, shape)
    return jelly(color, shape, depth)
}

/** Always-jelly surface; see [wfSurface] for the theme-aware version. */
fun Modifier.jelly(color: Color, shape: Shape, depth: Dp = 8.dp): Modifier {
    val deep = jellyShade(color)
    return this
        .shadow(depth, shape, clip = false, ambientColor = deep, spotColor = deep)
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            // Shading lives near the edges: a small gummy is round all over, a big sheet is a
            // flat slab with rounded rims, so the curve is capped in dp rather than relative.
            val curve = 56.dp.toPx()
            val h = size.height
            val top = (curve / h).coerceAtMost(0.5f)
            val body = Brush.verticalGradient(
                0f to lerp(color, Color.White, 0.38f),
                top to color,
                (1f - top).coerceAtLeast(top) to color,
                1f to lerp(color, deep, 0.28f),
            )
            val glossH = minOf(h * 0.45f, curve * 0.8f)
            val gloss = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.7f),
                1f to Color.White.copy(alpha = 0f),
                startY = 0f,
                endY = glossH,
            )
            val rimBand = (curve * 0.5f / h).coerceAtMost(0.4f)
            val topRim = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.9f),
                rimBand to Color.White.copy(alpha = 0f),
            )
            val bottomRim = Brush.verticalGradient(
                (1f - rimBand) to deep.copy(alpha = 0f),
                1f to deep.copy(alpha = 0.45f),
            )
            val rim = minOf(size.minDimension * 0.08f, 3.dp.toPx())
            val glossInset = minOf(size.minDimension * 0.12f, 24.dp.toPx())
            onDrawBehind {
                drawOutline(outline, body)
                // Specular highlight: a soft oval hugging the top, like light on a gummy bear.
                drawOval(
                    gloss,
                    topLeft = Offset(glossInset, h * 0.05f.coerceAtMost(4.dp.toPx() / h)),
                    size = Size(size.width - 2 * glossInset, glossH),
                )
                drawOutline(outline, topRim, style = Stroke(rim * 2))
                drawOutline(outline, bottomRim, style = Stroke(rim * 2.4f))
            }
        }
}

/** [jelly] for Canvas drawings (the phone's silicone case in the hero). */
fun DrawScope.drawJellyRoundRect(color: Color, topLeft: Offset, size: Size, corner: CornerRadius) {
    val deep = jellyShade(color)
    val top = topLeft.y
    val bottom = topLeft.y + size.height
    val curve = minOf(56.dp.toPx(), size.height / 2)
    drawRoundRect(
        Brush.verticalGradient(
            0f to lerp(color, Color.White, 0.4f),
            curve / size.height to color,
            1f - curve / size.height to color,
            1f to lerp(color, deep, 0.3f),
            startY = top,
            endY = bottom,
        ),
        topLeft, size, corner,
    )
    val rim = 3.dp.toPx()
    drawRoundRect(
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.85f), 1f to Color.White.copy(alpha = 0f), startY = top, endY = top + curve),
        topLeft, size, corner, style = Stroke(rim),
    )
    drawRoundRect(
        Brush.verticalGradient(0f to deep.copy(alpha = 0f), 1f to deep.copy(alpha = 0.5f), startY = bottom - curve, endY = bottom),
        topLeft, size, corner, style = Stroke(rim),
    )
}

/** The surface hue, deeper and a bit more saturated: jelly shadows and rims borrow this. */
fun jellyShade(color: Color): Color = lerp(color, Color(0xFF7A2E1E), 0.45f)

/**
 * A quick wobble when [selected] turns on: squash wide, stretch tall, settle (volume kept).
 * Only in the Album theme and only with animations on.
 */
@Composable
fun Modifier.jellyPop(selected: Boolean): Modifier {
    if (!LocalWfPalette.current.jelly || LocalReducedMotion.current) return this
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    var wasSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        if (selected && !wasSelected) {
            launch { sx.snapTo(1.16f); sx.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 420f)) }
            launch { sy.snapTo(0.84f); sy.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 380f)) }
        }
        wasSelected = selected
    }
    return graphicsLayer {
        scaleX = sx.value
        scaleY = sy.value
        transformOrigin = TransformOrigin(0.5f, 0.75f)
    }
}

/**
 * Drops in with a bounce, staggered by [index] (home cards "plop" onto the page).
 * Only in the Album theme and only with animations on.
 */
@Composable
fun Modifier.jellyEnter(index: Int): Modifier {
    if (!LocalWfPalette.current.jelly || LocalReducedMotion.current) return this
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(70L * index)
        t.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f))
    }
    return graphicsLayer {
        val v = t.value
        alpha = v.coerceIn(0f, 1f)
        // Lands wide and short, then springs up to shape.
        scaleX = 0.7f + 0.3f * v + (1f - v) * 0.1f
        scaleY = 0.55f + 0.45f * v
        translationY = (1f - v) * 60f
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}

/**
 * Soft candy-colored blobs drifting behind the page (Album theme only; nothing in Darkroom).
 * With reduced motion they stay still.
 */
@Composable
fun JellyBackdrop(modifier: Modifier = Modifier) {
    val palette = LocalWfPalette.current
    if (!palette.jelly) return
    // Read only while drawing, so the drift redraws the canvas without recomposing anything.
    val drift = if (LocalReducedMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "backdrop").animateFloat(
            0f, 1f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "drift",
        )
    }
    val blobs = palette.backdrop
    Canvas(modifier) {
        val phase = drift?.value ?: 0f
        drawRect(palette.bg)
        blobs.forEachIndexed { i, c ->
            val a = 2 * PI * (phase + i / blobs.size.toFloat())
            // Anchors spread over the page; each blob circles slowly around its own.
            val ax = size.width * listOf(0.12f, 0.88f, 0.3f, 0.78f, 0.5f)[i % 5]
            val ay = size.height * listOf(0.1f, 0.22f, 0.62f, 0.85f, 0.4f)[i % 5]
            val center = Offset(ax + cos(a).toFloat() * size.width * 0.05f, ay + sin(a).toFloat() * size.height * 0.04f)
            val r = size.minDimension * listOf(0.55f, 0.45f, 0.5f, 0.42f, 0.35f)[i % 5]
            drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, r), r, center)
        }
    }
}

/**
 * Material buttons as jelly in the Album theme: use together with [jellyButtonColors], which
 * clears the button's own flat container so this candy body shows through.
 */
@Composable
fun Modifier.jellyButton(color: Color, enabled: Boolean = true): Modifier {
    if (!LocalWfPalette.current.jelly) return this
    return jelly(if (enabled) color else color.copy(alpha = 0.35f), Wf.PillShape, if (enabled) 6.dp else 0.dp)
}

@Composable
fun jellyButtonColors(
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
): ButtonColors = if (LocalWfPalette.current.jelly) {
    ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = content,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = content.copy(alpha = 0.5f),
    )
} else {
    ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
}

/** Selection pill (chips, tabs): honey jelly when selected in Album, [idle] fill otherwise. */
@Composable
fun Modifier.jellyChip(selected: Boolean, shape: Shape = Wf.PillShape, idle: Color = Wf.Well): Modifier {
    if (!LocalWfPalette.current.jelly) return this
    val base = if (selected) jelly(Wf.Accent, shape, 4.dp) else background(idle, shape)
    return base.jellyPop(selected)
}
