package app.wireframephoto.ui.editor

import android.content.ActivityNotFoundException
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import app.wireframephoto.core.CollageState
import app.wireframephoto.data.ExportFormat
import app.wireframephoto.data.ImageSaver
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.ui.ExportStatus
import app.wireframephoto.ui.components.MorphingLoader
import app.wireframephoto.ui.components.jellyButton
import app.wireframephoto.ui.components.jellyButtonColors
import app.wireframephoto.ui.theme.AppTheme
import app.wireframephoto.ui.theme.LocalAppTheme
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.LocalReducedMotion
import app.wireframephoto.ui.theme.Wf
import kotlinx.coroutines.launch

private val Scales = listOf(1f, 0.5f)

@Composable
fun ExportDialogs(
    state: CollageState,
    loader: BitmapLoader,
    showOptions: Boolean,
    status: ExportStatus,
    onDismissOptions: () -> Unit,
    onExport: (Float, ExportFormat) -> Unit,
    onDismissStatus: () -> Unit,
) {
    if (showOptions) ExportOptionsDialog(state, onDismissOptions, onExport)

    when (status) {
        ExportStatus.Idle -> Unit
        is ExportStatus.Running -> FullScreen(onDismiss = {}) { Developing(status.progress, onDismissStatus) }
        is ExportStatus.Done -> FullScreen(onDismiss = onDismissStatus) { Reveal(status, loader, onDismissStatus) }
        is ExportStatus.Failed -> AlertDialog(
            onDismissRequest = onDismissStatus,
            containerColor = Wf.Card,
            title = { Text("저장하지 못했어요") },
            text = { Text(status.message) },
            confirmButton = { TextButton(onClick = onDismissStatus) { Text("확인") } },
        )
    }
}

@Composable
private fun FullScreen(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // The dialog is its own window: give its bar icons the same contrast as the activity's.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val light = LocalWfPalette.current.isLight
        SideEffect {
            window?.let { WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = light }
        }
        Box(Modifier.fillMaxSize().background(Wf.Bg.copy(alpha = 0.97f)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/** Darkroom metaphor while rendering: a breathing, morphing shape instead of a bare progress bar. */
@Composable
private fun Developing(progress: Float, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MorphingLoader(Modifier.size(96.dp))
        Text(if (LocalAppTheme.current == AppTheme.Album) "앨범에 담는 중…" else "현상하는 중…", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = Wf.AccentInk,
        )
        TextButton(onClick = onCancel) { Text("취소") }
    }
}

/**
 * The peak of the whole flow: the finished collage "develops" like instant film — from dark and
 * colorless to full color — inside a paper frame, with a confirm haptic.
 */
@Composable
private fun Reveal(status: ExportStatus.Done, loader: BitmapLoader, onClose: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    val image by produceState<ImageBitmap?>(null, status.uri) {
        value = loader.loadPreview(status.uri.toString(), 1440)?.asImageBitmap()
    }
    val develop = remember { Animatable(if (reduced) 1f else 0f) }
    val pop = remember { Animatable(if (reduced) 1f else 0.86f) }
    LaunchedEffect(image) {
        if (image == null) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        launch { pop.animateTo(1f, Wf.bouncy()) }
        develop.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag("export_done"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("완성!", style = MaterialTheme.typography.displaySmall, color = Wf.AccentInk)
        Spacer(Modifier.height(4.dp))
        Text(
            "갤러리의 ${ImageSaver.ALBUM} 앨범에 ${status.width}×${status.height}로 저장했어요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        // Instant-film print: white paper, thicker bottom edge.
        val aspect = status.width.toFloat() / status.height
        Box(Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) {
            Surface(
                color = Wf.Print,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        rotationZ = (1f - develop.value) * -3f
                    }
                    .shadow(30.dp, RoundedCornerShape(6.dp)),
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 34.dp)) {
                    val img = image
                    Box(Modifier.widthIn(max = 420.dp).aspectRatio(aspect).background(Wf.Bg)) {
                        if (img != null) {
                            val d = develop.value
                            Image(
                                img,
                                contentDescription = "저장된 콜라주",
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(developMatrix(d)),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Button(
                onClick = { context.startActivity(ImageSaver.shareIntent(status.uri, status.mimeType)) },
                shape = Wf.PillShape,
                colors = jellyButtonColors(),
                modifier = Modifier.weight(1f).height(52.dp).jellyButton(MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("공유하기", fontWeight = FontWeight.Bold)
            }
            if (status.isWallpaper) {
                FilledTonalButton(
                    onClick = {
                        try {
                            context.startActivity(ImageSaver.setAsIntent(status.uri, status.mimeType))
                        } catch (_: ActivityNotFoundException) {
                            // No handler; nothing else to offer.
                        }
                    },
                    shape = Wf.PillShape,
                    colors = if (LocalWfPalette.current.jelly) jellyButtonColors(Wf.Accent, Wf.OnAccent) else ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.weight(1f).height(52.dp).jellyButton(Wf.Accent),
                ) {
                    Icon(Icons.Outlined.Wallpaper, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("배경화면으로")
                }
            }
        }
        if (status.isWallpaper) {
            Spacer(Modifier.height(8.dp))
            Text(
                "설정 화면에서 커버 화면(접었을 때) 또는 홈·잠금 화면을 고르면 돼요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onClose) { Text("계속 편집") }
    }
}

/** d = 0: dark, colorless latent image; d = 1: the real photo. */
private fun developMatrix(d: Float): ColorMatrix {
    val m = ColorMatrix().apply { setToSaturation(d) }
    val brightness = 0.35f + 0.65f * d
    return m.apply { timesAssign(ColorMatrix().apply { setToScale(brightness, brightness, brightness, 1f) }) }
}

@Composable
private fun ExportOptionsDialog(
    state: CollageState,
    onDismiss: () -> Unit,
    onExport: (Float, ExportFormat) -> Unit,
) {
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    var scaleIndex by rememberSaveable { mutableIntStateOf(0) }
    val formats = ExportFormat.entries
    val segmentColors = SegmentedButtonDefaults.colors(
        activeContainerColor = Wf.Accent,
        activeContentColor = Wf.OnAccent,
        inactiveContainerColor = Wf.Well,
        inactiveContentColor = Wf.TextDim,
        activeBorderColor = Wf.AccentInk,
        inactiveBorderColor = Wf.Line,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Wf.Card,
        shape = Wf.SheetShape,
        title = { Text("갤러리에 저장") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("형식", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    formats.forEachIndexed { i, f ->
                        SegmentedButton(
                            selected = formatIndex == i,
                            onClick = { formatIndex = i },
                            shape = SegmentedButtonDefaults.itemShape(i, formats.size),
                            colors = segmentColors,
                        ) { Text(f.name) }
                    }
                }
                Text("크기", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(56.dp)) {
                    Scales.forEachIndexed { i, s ->
                        val (w, h) = state.preset.sizeAt(s)
                        SegmentedButton(
                            selected = scaleIndex == i,
                            onClick = { scaleIndex = i },
                            shape = SegmentedButtonDefaults.itemShape(i, Scales.size),
                            colors = segmentColors,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (i == 0) "고화질" else "가볍게", fontWeight = FontWeight.Bold)
                                Text("${w}×${h}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Text(
                    if (state.preset.isWallpaper) "고화질은 화면 해상도와 똑같아서 배경화면으로 딱 맞아요. JPEG는 용량이 작고, PNG는 화질 손실이 없어요."
                    else "JPEG는 용량이 작고, PNG는 화질 손실이 없어요. 공유만 할 거라면 '가볍게'도 충분해요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(Scales[scaleIndex], formats[formatIndex]) },
                shape = Wf.PillShape,
                colors = jellyButtonColors(),
                modifier = Modifier.jellyButton(MaterialTheme.colorScheme.primary).testTag("export_confirm"),
            ) { Text("저장", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
