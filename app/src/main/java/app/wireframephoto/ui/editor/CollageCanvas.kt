package app.wireframephoto.ui.editor

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.wireframephoto.core.CellTransform
import app.wireframephoto.core.CollageGeometry
import app.wireframephoto.core.CollageState
import app.wireframephoto.core.CropMath
import app.wireframephoto.core.PxRect
import app.wireframephoto.core.Templates
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.ui.components.animateCells
import app.wireframephoto.ui.components.wfSurface
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class CanvasActions(
    val onTapCell: (Int) -> Unit,
    val onTapEmptyCell: (Int) -> Unit,
    val onTapOutside: () -> Unit,
    val onDoubleTapCell: (Int) -> Unit,
    val onGestureStart: () -> Unit,
    val onTransform: (Int, CellTransform) -> Unit,
    val onGestureEnd: () -> Unit,
    val onSwap: (Int, Int) -> Unit,
    /** Photos dropped from another app; cell is -1 when dropped outside every cell. */
    val onDropPhotos: (Int, List<Uri>) -> Unit,
)

private enum class Decision { Tap, Transform, LongPress }

/**
 * Interactive WYSIWYG preview. Uses the same [CollageGeometry] and [CropMath] as the exporter so
 * what the user frames here is exactly what gets saved.
 */
@Composable
fun CollageCanvas(
    state: CollageState,
    selectedCell: Int?,
    loader: BitmapLoader,
    actions: CanvasActions,
    modifier: Modifier = Modifier,
    /** Wallpaper purposes: draw the canvas inside a phone bezel so it reads as "your screen". */
    deviceFrame: Boolean = false,
    /** Number badges that match the tiles in the 사진 tool. */
    showNumbers: Boolean = false,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val addLabelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val addLabel = remember(textMeasurer, addLabelStyle) { textMeasurer.measure("사진 추가", addLabelStyle) }
    val badgeStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
    val badgeLabels = remember(textMeasurer, badgeStyle) {
        (1..Templates.MAX_PHOTOS).map { textMeasurer.measure("$it", badgeStyle) }
    }
    val haptics = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val colors = MaterialTheme.colorScheme

    // Preview bitmaps keyed by URI, seeded from the app-wide cache so fold/unfold doesn't flicker.
    val previews = remember { mutableStateMapOf<String, ImageBitmap>() }
    val visibleUris = state.slots.take(state.template.count).mapNotNull { it.uri }.distinct()
    visibleUris.forEach { uri ->
        if (uri !in previews) loader.cachedPreview(uri)?.let { previews[uri] = it.asImageBitmap() }
    }
    LaunchedEffect(visibleUris) {
        visibleUris.filter { it !in previews }.forEach { uri ->
            launch { loader.loadPreview(uri)?.let { previews[uri] = it.asImageBitmap() } }
        }
    }

    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoverCell by remember { mutableStateOf<Int?>(null) }
    var dropHoverCell by remember { mutableStateOf<Int?>(null) }
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier) {
        val styled = LocalWfPalette.current.styled
        val bezelDp = 7.dp
        val bezel = if (deviceFrame) with(density) { bezelDp.toPx() } else 0f
        val screenRadius = 14.dp
        val frame = CollageGeometry.fit(
            state.preset.aspect,
            constraints.maxWidth - 2 * bezel,
            constraints.maxHeight - 2 * bezel,
        ).let { PxRect(it.left + bezel, it.top + bezel, it.right + bezel, it.bottom + bezel) }
        val canvasW = frame.width
        val canvasH = frame.height
        val rects = remember(state.template, state.style, canvasW, canvasH) {
            CollageGeometry.cellRects(state.template, canvasW, canvasH, state.style)
        }

        // Cells glide to new positions when the layout or spacing changes. Animating in normalized
        // canvas space means resizing the window (fold/unfold) snaps instead of wobbling.
        val normTargets = rects.map { Rect(it.left / canvasW, it.top / canvasH, it.right / canvasW, it.bottom / canvasH) }
        val drawRects = animateCells(normTargets).map {
            PxRect(it.left * canvasW, it.top * canvasH, it.right * canvasW, it.bottom * canvasH)
        }

        val currentState by rememberUpdatedState(state)
        val currentRects by rememberUpdatedState(rects)
        val currentActions by rememberUpdatedState(actions)
        fun hit(p: Offset) = currentRects.indexOfFirst { it.contains(p.x, p.y) }

        val dropTarget = remember {
            object : DragAndDropTarget {
                override fun onMoved(event: DragAndDropEvent) {
                    val e = event.toAndroidDragEvent()
                    dropHoverCell = hit(Offset(e.x, e.y) - positionInRoot).takeIf { it >= 0 }
                }

                override fun onExited(event: DragAndDropEvent) {
                    dropHoverCell = null
                }

                override fun onEnded(event: DragAndDropEvent) {
                    dropHoverCell = null
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val e = event.toAndroidDragEvent()
                    dropHoverCell = null
                    // Content URIs from another app are only readable after requesting permission.
                    activity?.requestDragAndDropPermissions(e)
                    val clip = e.clipData ?: return false
                    val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
                    if (uris.isEmpty()) return false
                    currentActions.onDropPhotos(hit(Offset(e.x, e.y) - positionInRoot), uris)
                    return true
                }
            }
        }

        if (deviceFrame) {
            Box(
                Modifier
                    .offset { IntOffset((frame.left - bezel).roundToInt(), (frame.top - bezel).roundToInt()) }
                    .size(with(density) { (canvasW + 2 * bezel).toDp() }, with(density) { (canvasH + 2 * bezel).toDp() })
                    .then(
                        if (styled) Modifier.wfSurface(Wf.DeviceBody, RoundedCornerShape(screenRadius + bezelDp), 14.dp)
                        else Modifier
                            .background(Wf.DeviceBody, RoundedCornerShape(screenRadius + bezelDp))
                            .border(1.dp, Wf.DeviceEdge, RoundedCornerShape(screenRadius + bezelDp)),
                    ),
            )
        }
        Canvas(
            Modifier
                .offset { IntOffset(frame.left.roundToInt(), frame.top.roundToInt()) }
                .size(with(density) { canvasW.toDp() }, with(density) { canvasH.toDp() })
                .then(if (deviceFrame) Modifier.clip(RoundedCornerShape(screenRadius)) else Modifier)
                .onGloballyPositioned { positionInRoot = it.positionInRoot() }
                .testTag("collage_canvas")
                .semantics { contentDescription = "콜라주 미리보기, 사진 ${state.photoCount}장" }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { e -> e.mimeTypes().any { it.startsWith("image/") } },
                    target = dropTarget,
                )
                .pointerInput(Unit) {
                    var lastTapTime = 0L
                    var lastTapCell = -1
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cell = hit(down.position)
                        if (cell < 0) {
                            currentActions.onTapOutside()
                            return@awaitEachGesture
                        }
                        val decision = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            awaitDecision(viewConfiguration.touchSlop)
                        } ?: Decision.LongPress

                        when (decision) {
                            Decision.Tap -> {
                                val isEmpty = currentState.slot(cell).uri == null
                                val isDouble = cell == lastTapCell &&
                                    down.uptimeMillis - lastTapTime < viewConfiguration.doubleTapTimeoutMillis
                                when {
                                    isEmpty -> currentActions.onTapEmptyCell(cell)
                                    isDouble -> currentActions.onDoubleTapCell(cell)
                                    else -> currentActions.onTapCell(cell)
                                }
                                lastTapTime = if (isDouble) 0L else down.uptimeMillis
                                lastTapCell = cell
                            }

                            Decision.Transform -> {
                                val uri = currentState.slot(cell).uri
                                val image = uri?.let { previews[it] }
                                if (image != null) {
                                    currentActions.onGestureStart()
                                    currentActions.onTapCell(cell)
                                    val aspect = image.width.toFloat() / image.height
                                    do {
                                        val event = awaitPointerEvent()
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        if (zoom != 1f || pan != Offset.Zero) {
                                            val r = currentRects[cell]
                                            val focus = event.calculateCentroid(useCurrent = false)
                                            val next = CropMath.applyGesture(
                                                currentState.slot(cell).transform, aspect, r.width, r.height,
                                                pan.x, pan.y, zoom, focus.x - r.left, focus.y - r.top,
                                            )
                                            currentActions.onTransform(cell, next)
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } while (event.changes.any { it.pressed })
                                    currentActions.onGestureEnd()
                                }
                            }

                            Decision.LongPress -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragFrom = cell
                                dragPosition = down.position
                                hoverCell = cell
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    dragPosition = change.position
                                    hoverCell = hit(change.position).takeIf { it >= 0 }
                                    change.consume()
                                    if (!change.pressed) break
                                }
                                val target = hoverCell
                                dragFrom = null
                                hoverCell = null
                                if (target != null && target != cell) {
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    currentActions.onSwap(cell, target)
                                } else {
                                    currentActions.onTapCell(cell)
                                }
                            }
                        }
                    }
                },
        ) {
            drawRect(Color(state.style.backgroundColor))
            val radius = state.style.cornerRadiusPx(canvasW, canvasH)
            drawRects.forEachIndexed { i, r ->
                // Cells grow from zero size when they first appear; nothing to draw until they have area.
                if (r.width < 1f || r.height < 1f) return@forEachIndexed
                val slot = state.slot(i)
                val image = slot.uri?.let { previews[it] }
                val cellPath = Path().apply {
                    addRoundRect(RoundRect(r.left, r.top, r.right, r.bottom, CornerRadius(radius)))
                }
                clipPath(cellPath) {
                    if (image != null) {
                        drawCell(image, r, slot.transform, alpha = if (i == dragFrom) 0.3f else 1f)
                    } else {
                        drawRect(colors.surfaceVariant, Offset(r.left, r.top), Size(r.width, r.height))
                        if (slot.uri == null) {
                            val showLabel = min(r.width, r.height) > addLabel.size.width + 16.dp.toPx()
                            val lift = if (showLabel) addLabel.size.height / 2f + 4.dp.toPx() else 0f
                            drawPlus(r, colors.onSurfaceVariant.copy(alpha = 0.7f), lift)
                            if (showLabel) {
                                drawText(
                                    addLabel,
                                    topLeft = Offset(r.centerX - addLabel.size.width / 2f, r.centerY + lift / 2f + 4.dp.toPx()),
                                )
                            }
                        }
                    }
                }
                val highlight = when (i) {
                    hoverCell, dropHoverCell -> colors.tertiary
                    selectedCell -> colors.primary
                    else -> null
                }
                if (highlight != null && !(i == hoverCell && i == dragFrom) && r.width > 8f && r.height > 8f) {
                    val stroke = 3.dp.toPx()
                    drawRoundRect(
                        highlight,
                        topLeft = Offset(r.left + stroke / 2, r.top + stroke / 2),
                        size = Size(r.width - stroke, r.height - stroke),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(stroke),
                    )
                }
            }
            if (showNumbers) {
                drawRects.forEachIndexed { i, r ->
                    val label = badgeLabels.getOrNull(i) ?: return@forEachIndexed
                    val radiusPx = 11.dp.toPx()
                    val c = Offset(r.left + radiusPx + 6.dp.toPx(), r.top + radiusPx + 6.dp.toPx())
                    drawCircle(Color.Black.copy(alpha = 0.6f), radiusPx, c)
                    drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
                }
            }
            if (deviceFrame) {
                // Punch-hole camera, so the preview reads unmistakably as a phone screen.
                drawCircle(Color.Black, 4.dp.toPx(), Offset(canvasW / 2f, 12.dp.toPx()))
            }
            // Floating copy of the photo being dragged for a swap.
            val from = dragFrom
            val fromImage = from?.let { state.slot(it).uri }?.let { previews[it] }
            if (from != null && fromImage != null) {
                val r = drawRects[from]
                val scale = min(1f, 120.dp.toPx() / min(r.width, r.height))
                val w = r.width * scale
                val h = r.height * scale
                val floating = PxRect(dragPosition.x - w / 2, dragPosition.y - h / 2, dragPosition.x + w / 2, dragPosition.y + h / 2)
                drawCell(fromImage, floating, state.slot(from).transform, alpha = 0.85f)
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitDecision(touchSlop: Float): Decision {
    var pan = Offset.Zero
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.count { it.pressed }
        if (pressed == 0) return Decision.Tap
        if (pressed >= 2) return Decision.Transform
        pan += event.calculatePan()
        if (pan.getDistance() > touchSlop) return Decision.Transform
    }
}

private fun DrawScope.drawCell(image: ImageBitmap, r: PxRect, transform: CellTransform, alpha: Float) {
    val src = CropMath.sourceRect(image.width, image.height, r.width, r.height, transform)
    val sx = src.left.roundToInt().coerceIn(0, image.width - 1)
    val sy = src.top.roundToInt().coerceIn(0, image.height - 1)
    drawImage(
        image = image,
        srcOffset = IntOffset(sx, sy),
        srcSize = IntSize(
            src.width.roundToInt().coerceIn(1, image.width - sx),
            src.height.roundToInt().coerceIn(1, image.height - sy),
        ),
        dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()),
        dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawPlus(r: PxRect, color: Color, lift: Float) {
    val arm = min(min(r.width, r.height) * 0.1f, 14.dp.toPx())
    val stroke = 2.5.dp.toPx()
    val cy = r.centerY - lift / 2f
    drawLine(color, Offset(r.centerX - arm, cy), Offset(r.centerX + arm, cy), stroke, StrokeCap.Round)
    drawLine(color, Offset(r.centerX, cy - arm), Offset(r.centerX, cy + arm), stroke, StrokeCap.Round)
}
