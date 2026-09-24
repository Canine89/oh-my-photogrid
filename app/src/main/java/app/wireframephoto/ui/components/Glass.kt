package app.wireframephoto.ui.components

import android.os.Build
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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.wireframephoto.ui.theme.LocalReducedMotion
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * "Warm liquid glass" for the 가족 앨범 theme (iOS 26-era translucent UI, warmed up for family
 * photos). The page behind is warm color — or, in the editor, the user's own photos, heavily
 * blurred — and every surface is a pane of frosted glass that lets that color through:
 * - a real backdrop blur (Haze; Android 12+, a stronger translucent fill below that)
 * - a translucent tint of the surface color
 * - a thin specular rim, bright along the top edge and fading down, and a soft neutral shadow
 * In Darkroom every helper here falls back to the plain, flat behavior.
 */

/** Blur source for glass surfaces in this window (set by the screen that draws the backdrop). */
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Glass (Album theme) or flat fill (Darkroom) for a surface of [color] in [shape]. */
@Composable
fun Modifier.wfSurface(color: Color, shape: Shape, depth: Dp = 8.dp, tint: Float = 0.5f): Modifier {
    val palette = LocalWfPalette.current
    if (!palette.glass) return background(color, shape)
    // Accent glass carries white text, so it stays nearly opaque whatever is behind it.
    return glass(color, shape, depth, if (color == palette.accent) maxOf(tint, 0.92f) else tint)
}

/**
 * A pane of frosted glass tinted with [color] at [tint] opacity. Without a blur source (dialogs
 * live in their own window) or before Android 12, the tint is raised so text stays readable.
 */
@Composable
fun Modifier.glass(color: Color, shape: Shape, depth: Dp = 8.dp, tint: Float = 0.5f): Modifier {
    val state = LocalGlassState.current
    val canBlur = state != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val fill = color.copy(alpha = if (canBlur) tint else max(tint, 0.9f))
    val shadowColor = Color(0x552A1A10)
    val base = this
        .shadow(depth, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
        .clip(shape)
    val frosted = if (canBlur) {
        base.hazeBlur(
            HazeInput.Sources(state!!),
            HazeBlurStyle {
                blurRadius(28.dp)
                noiseFactor(0.04f)
                colorEffects(listOf(HazeColorEffect.tint(fill)))
            },
        )
    } else {
        base.background(fill)
    }
    return frosted.glassRim(shape)
}

/** The specular edge that makes a translucent pane read as glass rather than as fog. */
private fun Modifier.glassRim(shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val band = minOf(size.height, 48.dp.toPx())
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.95f),
        (band / size.height) to Color.White.copy(alpha = 0.35f),
        1f to Color.White.copy(alpha = 0.2f),
    )
    // A faint sheen on the upper part of the pane, as light falls on a curved glass edge.
    val sheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.28f),
        1f to Color.White.copy(alpha = 0f),
        startY = 0f,
        endY = band,
    )
    val stroke = 1.2.dp.toPx()
    onDrawWithContent {
        drawOutline(outline, sheen)
        drawContent()
        drawOutline(outline, rim, style = Stroke(stroke * 2))
    }
}

/**
 * Material buttons as glass in the Album theme: use together with [glassButtonColors], which
 * clears the button's own container so the tinted glass shows. Buttons are nearly opaque glass so
 * their label keeps full contrast.
 */
@Composable
fun Modifier.glassButton(color: Color, enabled: Boolean = true): Modifier {
    if (!LocalWfPalette.current.glass) return this
    return glass(if (enabled) color else color.copy(alpha = 0.35f), Wf.PillShape, if (enabled) 6.dp else 0.dp, tint = 0.92f)
}

@Composable
fun glassButtonColors(
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
): ButtonColors = if (LocalWfPalette.current.glass) {
    ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = content,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = content.copy(alpha = 0.5f),
    )
} else {
    ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
}

/** Selection pill (chips, tabs): accent glass when selected in Album, [idle] glass otherwise. */
@Composable
fun Modifier.glassChip(selected: Boolean, shape: Shape = Wf.PillShape, idle: Color = Wf.Raised): Modifier {
    if (!LocalWfPalette.current.glass) return this
    return when {
        selected -> glass(Wf.Accent, shape, 4.dp, tint = 0.92f)
        idle.alpha == 0f -> this
        else -> glass(idle, shape, 0.dp, tint = 0.4f)
    }
}

/** Rises and fades in, staggered by [index] (home cards settle onto the page). Album only. */
@Composable
fun Modifier.glassEnter(index: Int): Modifier {
    if (!LocalWfPalette.current.glass || LocalReducedMotion.current) return this
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60L * index)
        t.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 180f))
    }
    return graphicsLayer {
        alpha = t.value.coerceIn(0f, 1f)
        translationY = (1f - t.value) * 36.dp.toPx()
    }
}

/**
 * The warm color behind the glass (Album theme only; nothing in Darkroom). With [photos], the
 * user's own pictures, blurred into light; otherwise soft warm blobs that drift slowly.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, photos: List<ImageBitmap> = emptyList()) {
    val palette = LocalWfPalette.current
    if (!palette.glass) return
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Read only while drawing, so the drift redraws without recomposing anything.
    val drift = if (LocalReducedMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "backdrop").animateFloat(
            0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "drift",
        )
    }
    Box(modifier) {
        Canvas(
            Modifier.matchParentSize()
                .then(if (photos.isNotEmpty() && canBlur) Modifier.blur(90.dp, BlurredEdgeTreatment.Rectangle) else Modifier),
        ) {
            drawRect(palette.bg)
            if (photos.isNotEmpty()) {
                // Photos tile the page (cover-cropped), then the blur turns them into pure color.
                val cols = if (photos.size >= 2) 2 else 1
                val rows = (photos.size + cols - 1) / cols
                val cw = size.width / cols
                val ch = size.height / rows
                photos.forEachIndexed { i, img ->
                    val scale = max(cw / img.width, ch / img.height)
                    val sw = (cw / scale).roundToInt().coerceIn(1, img.width)
                    val sh = (ch / scale).roundToInt().coerceIn(1, img.height)
                    drawImage(
                        img,
                        srcOffset = IntOffset((img.width - sw) / 2, (img.height - sh) / 2),
                        srcSize = IntSize(sw, sh),
                        dstOffset = IntOffset(((i % cols) * cw).roundToInt(), ((i / cols) * ch).roundToInt()),
                        dstSize = IntSize(cw.roundToInt() + 1, ch.roundToInt() + 1),
                    )
                }
            } else {
                val phase = drift?.value ?: 0f
                palette.backdrop.forEachIndexed { i, c ->
                    val a = 2 * PI * (phase + i / palette.backdrop.size.toFloat())
                    val ax = size.width * listOf(0.1f, 0.9f, 0.35f, 0.8f, 0.5f)[i % 5]
                    val ay = size.height * listOf(0.08f, 0.25f, 0.6f, 0.9f, 0.42f)[i % 5]
                    val center = Offset(ax + cos(a).toFloat() * size.width * 0.06f, ay + sin(a).toFloat() * size.height * 0.05f)
                    val r = size.maxDimension * listOf(0.55f, 0.5f, 0.5f, 0.45f, 0.38f)[i % 5]
                    drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, r), r, center)
                }
            }
        }
        // A clear white wash keeps text on glass readable over any photo without tinting it.
        val wash = if (photos.isEmpty()) 0.12f else if (canBlur) 0.3f else 0.75f
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = wash)))
    }
}
