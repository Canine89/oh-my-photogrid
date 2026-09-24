package app.wireframephoto.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import app.wireframephoto.ui.theme.LocalReducedMotion
import app.wireframephoto.ui.theme.Wf
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.min

/**
 * Animates a list of rects (in any coordinate space) toward [targets] with an expressive spring.
 * Rects that appear grow out of their own center; with reduced motion they snap.
 */
@Composable
fun animateCells(targets: List<Rect>): List<Rect> {
    val reduced = LocalReducedMotion.current
    val animated = remember { mutableStateListOf<Animatable<Rect, AnimationVector4D>>() }
    LaunchedEffect(targets) {
        while (animated.size > targets.size) animated.removeAt(animated.lastIndex)
        targets.forEachIndexed { i, target ->
            if (i >= animated.size) animated.add(Animatable(Rect(target.center, 0f), Rect.VectorConverter))
            launch { if (reduced) animated[i].snapTo(target) else animated[i].animateTo(target, Wf.bouncy()) }
        }
    }
    return targets.indices.map { i -> animated.getOrNull(i)?.value ?: targets[i] }
}

/** Squish-on-press (Material 3 Expressive feel) for any clickable surface. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Wf.bouncy(), label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Shape-morphing loading indicator (the Material 3 Expressive "loading indicator" idea, built on
 * the stable graphics-shapes library): cookie → rounded square → soft star → circle, rotating.
 */
@Composable
fun MorphingLoader(modifier: Modifier = Modifier, color: Color = Wf.Lime) {
    val shapes = remember {
        listOf(
            RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(0.1f), innerRounding = CornerRounding(0.1f)),
            RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.35f)),
            RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.6f, rounding = CornerRounding(0.18f), innerRounding = CornerRounding(0.12f)),
            RoundedPolygon.circle(numVertices = 8),
        ).map { it.normalized() }
    }
    val morphs = remember { shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) } }
    val androidPath = remember { android.graphics.Path() }
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "loader")
    val phase by transition.animateFloat(
        0f, shapes.size.toFloat(),
        infiniteRepeatable(tween(shapes.size * 700, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "spin")

    Canvas(modifier) {
        val p = if (reduced) 0f else phase
        val index = floor(p).toInt() % morphs.size
        // Hold each shape briefly, then morph with an ease so it reads as "breathing".
        val local = ((p - floor(p)) * 1.4f - 0.2f).coerceIn(0f, 1f)
        androidPath.rewind()
        morphs[index].toPath(FastOutSlowInEasing.transform(local), androidPath)
        val side = min(size.width, size.height)
        translate((size.width - side) / 2f, (size.height - side) / 2f) {
            rotate(if (reduced) 0f else spin, pivot = androidx.compose.ui.geometry.Offset(side / 2f, side / 2f)) {
                scale(side, side, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(androidPath.asComposePath(), color)
                }
            }
        }
    }
}
