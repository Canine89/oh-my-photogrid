package app.wireframephoto.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.wireframephoto.core.CollageState
import app.wireframephoto.core.CollageStyle
import app.wireframephoto.core.StylePresets
import app.wireframephoto.core.Templates
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.ui.Purposes
import app.wireframephoto.ui.components.PurposeRow
import app.wireframephoto.ui.components.jellyButton
import app.wireframephoto.ui.components.jellyButtonColors
import app.wireframephoto.ui.components.jellyChip
import app.wireframephoto.ui.components.jellyPop
import app.wireframephoto.ui.components.pressScale
import app.wireframephoto.ui.components.wfSurface
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf

/** The four editing tools, in the order people use them. */
enum class Tool(val title: String, val description: String, val icon: ImageVector) {
    Layout("배치", "사진을 어떤 모양으로 나눌지 골라요", Icons.Outlined.GridView),
    Photos("사진", "사진을 넣고, 바꾸고, 빼요", Icons.Outlined.PhotoLibrary),
    Style("꾸미기", "간격·모서리·배경색을 바꿔요", Icons.Outlined.Palette),
    Size("크기", "어디에 쓸지 고르면 크기가 맞춰져요", Icons.Outlined.AspectRatio),
}

/** Everything the tool sections need from the editor, bundled to keep call sites short. */
class ToolCallbacks(
    val onSelectTemplate: (String) -> Unit,
    val onSelectPreset: (String) -> Unit,
    val onStyleChange: (CollageStyle, Boolean) -> Unit,
    val onStyleDragEnd: () -> Unit,
    val onSelectCell: (Int) -> Unit,
    val onPickForCell: (Int) -> Unit,
    val onAddPhotos: () -> Unit,
    val onShuffle: () -> Unit,
)

// ---------------------------------------------------------------------------------------------
// Containers
// ---------------------------------------------------------------------------------------------

/** Unfolded screen: a floating rounded panel with a pill tool switcher on top. */
@Composable
fun ToolSidePanel(
    tool: Tool,
    onTool: (Tool) -> Unit,
    state: CollageState,
    selectedCell: Int?,
    loader: BitmapLoader,
    callbacks: ToolCallbacks,
    modifier: Modifier = Modifier,
) {
    val jelly = LocalWfPalette.current.jelly
    Surface(
        modifier.wfSurface(Wf.Card, Wf.SheetShape, 16.dp),
        shape = Wf.SheetShape,
        color = if (jelly) Color.Transparent else Wf.Card,
        border = if (jelly) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.padding(10.dp).fillMaxWidth().background(Wf.Bg, Wf.PillShape).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Tool.entries.forEach { t -> ToolPill(t, tool == t, Modifier.weight(1f)) { onTool(t) } }
            }
            ToolDescription(tool)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ToolSection(tool, state, selectedCell, loader, callbacks)
            }
        }
    }
}

/**
 * Folded (cover) screen: a floating pill toolbar — four tools plus Shuffle. It fades back while
 * the user is manipulating photos ([dimmed]) so nothing competes with the canvas.
 */
@Composable
fun FloatingToolBar(
    openTool: Tool?,
    onToggle: (Tool) -> Unit,
    onShuffle: () -> Unit,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (dimmed) 0.25f else 1f, Wf.snappy(), label = "toolbarAlpha")
    val jelly = LocalWfPalette.current.jelly
    Surface(
        modifier = modifier.alpha(alpha).then(
            if (jelly) Modifier.wfSurface(Wf.Raised, Wf.PillShape, 16.dp)
            else Modifier.shadow(18.dp, Wf.PillShape, ambientColor = Color.Black, spotColor = Color.Black),
        ),
        shape = Wf.PillShape,
        color = if (jelly) Color.Transparent else Wf.Raised.copy(alpha = 0.97f),
        border = if (jelly) null else BorderStroke(1.dp, Wf.Line.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tool.entries.forEach { t -> ToolPill(t, openTool == t, Modifier.width(66.dp)) { onToggle(t) } }
            Spacer(Modifier.width(4.dp))
            ShuffleButton(onShuffle)
        }
    }
}

/** Unfolded screen: Shuffle floats over the canvas. */
@Composable
fun ShufflePill(onShuffle: () -> Unit, dimmed: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (dimmed) 0.25f else 1f, Wf.snappy(), label = "shuffleAlpha")
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val jelly = LocalWfPalette.current.jelly
    Surface(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onShuffle() },
        shape = Wf.PillShape,
        color = if (jelly) Color.Transparent else Wf.Raised.copy(alpha = 0.97f),
        border = if (jelly) null else BorderStroke(1.dp, Wf.Line.copy(alpha = 0.6f)),
        interactionSource = interaction,
        modifier = modifier.alpha(alpha).pressScale(interaction)
            .then(if (jelly) Modifier.wfSurface(Wf.Raised, Wf.PillShape, 14.dp) else Modifier.shadow(14.dp, Wf.PillShape))
            .testTag("shuffle"),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Casino, contentDescription = null, tint = Wf.AccentInk)
            Spacer(Modifier.width(8.dp))
            Text("섞어 보기", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ShuffleButton(onShuffle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val jelly = LocalWfPalette.current.jelly
    Surface(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onShuffle() },
        shape = CircleShape,
        color = if (jelly) Color.Transparent else Wf.Accent,
        contentColor = Wf.OnAccent,
        interactionSource = interaction,
        modifier = Modifier.size(52.dp).pressScale(interaction, 0.88f).wfSurface(Wf.Accent, CircleShape, 6.dp).testTag("shuffle")
            .semantics { contentDescription = "섞어 보기: 배치와 스타일을 무작위로" },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Casino, contentDescription = null) }
    }
}

/** One tool: icon in a pill that lights up lime when active, label underneath. */
@Composable
private fun ToolPill(tool: Tool, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Wf.Accent else Color.Transparent, Wf.snappy(), label = "pillBg")
    val fg by animateColorAsState(if (selected) Wf.OnAccent else Wf.TextDim, Wf.snappy(), label = "pillFg")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pressScale(interaction, 0.9f)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .testTag("tool_${tool.name}")
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(width = 52.dp, height = 30.dp)
                .then(if (LocalWfPalette.current.jelly) Modifier.jellyChip(selected, idle = Color.Transparent).clip(Wf.PillShape) else Modifier.background(bg, Wf.PillShape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tool.icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            tool.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Wf.Text else Wf.TextDim,
        )
    }
}

@Composable
fun ToolDescription(tool: Tool, modifier: Modifier = Modifier) {
    Text(
        tool.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 18.dp, vertical = 2.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------------------------

@Composable
fun ToolSection(
    tool: Tool,
    state: CollageState,
    selectedCell: Int?,
    loader: BitmapLoader,
    callbacks: ToolCallbacks,
) {
    when (tool) {
        Tool.Layout -> LayoutSection(state, loader, callbacks.onSelectTemplate)
        Tool.Photos -> PhotosSection(state, selectedCell, loader, callbacks)
        Tool.Style -> StyleSection(state.style, callbacks.onStyleChange, callbacks.onStyleDragEnd)
        Tool.Size -> SizeSection(state, callbacks.onSelectPreset)
    }
}

@Composable
private fun pillChipColors() = if (LocalWfPalette.current.jelly) {
    // The jelly body is drawn by Modifier.jellyChip; the chip itself stays clear.
    FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = Wf.TextDim,
        selectedContainerColor = Color.Transparent,
        selectedLabelColor = Wf.OnAccent,
    )
} else {
    FilterChipDefaults.filterChipColors(
        containerColor = Wf.Well,
        labelColor = Wf.TextDim,
        selectedContainerColor = Wf.Accent,
        selectedLabelColor = Wf.OnAccent,
    )
}

@Composable
private fun LayoutSection(state: CollageState, loader: BitmapLoader, onSelect: (String) -> Unit) {
    var count by rememberSaveable(state.template.count) { mutableIntStateOf(state.template.count) }
    val haptics = LocalHapticFeedback.current
    // Keep thumbnails readable even for very tall/wide canvases.
    val thumbAspect = state.preset.aspect.coerceIn(0.6f, 1.6f)
    // Thumbnails show the user's own photos; start from the cache and fill in as previews finish loading.
    val uris = (0 until Templates.MAX_PHOTOS).map { state.slot(it).uri }
    val photos by produceState(uris.map { u -> u?.let { loader.cachedPreview(it)?.asImageBitmap() } }, uris) {
        value = uris.map { u -> u?.let { loader.loadPreview(it)?.asImageBitmap() } }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 84.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("layout_grid"),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..Templates.MAX_PHOTOS).forEach { n ->
                    FilterChip(
                        selected = count == n,
                        onClick = { count = n },
                        label = { Text("${n}칸") },
                        shape = Wf.PillShape,
                        colors = pillChipColors(),
                        border = null,
                        modifier = Modifier.jellyChip(count == n),
                    )
                }
            }
        }
        items(Templates.forCount(count), key = { it.id }) { t ->
            val selected = t.id == state.templateId
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .pressScale(interaction, 0.92f)
                    .jellyPop(selected)
                    .wfSurface(if (selected) Wf.Raised else Wf.Well, RoundedCornerShape(16.dp), if (selected) 8.dp else 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(if (selected) BorderStroke(2.5.dp, Wf.AccentInk) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
                    .clickable(interactionSource = interaction, indication = null) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(t.id)
                    }
                    .semantics {
                        contentDescription = "${t.count}칸 배치"
                        this.selected = selected
                    }
                    .testTag("template_${t.id}")
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiveTemplateThumbnail(t, thumbAspect, photos, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PhotosSection(state: CollageState, selectedCell: Int?, loader: BitmapLoader, callbacks: ToolCallbacks) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 72.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.template.count) { i ->
            PhotoTile(
                number = i + 1,
                uri = state.slot(i).uri,
                selected = selectedCell == i,
                loader = loader,
                onClick = { if (state.slot(i).uri == null) callbacks.onPickForCell(i) else callbacks.onSelectCell(i) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = callbacks.onAddPhotos,
                    enabled = state.photoCount < Templates.MAX_PHOTOS,
                    shape = Wf.PillShape,
                    colors = if (LocalWfPalette.current.jelly) jellyButtonColors(Wf.Accent, Wf.OnAccent) else ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .jellyButton(Wf.Accent, enabled = state.photoCount < Templates.MAX_PHOTOS).testTag("add_photos"),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("사진 더 넣기 (${state.photoCount}/${Templates.MAX_PHOTOS}장)")
                }
                Text(
                    "사진을 누르면 캔버스에서 선택돼요. 번호는 캔버스의 칸 번호와 같아요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(number: Int, uri: String?, selected: Boolean, loader: BitmapLoader, onClick: () -> Unit) {
    val image by produceState(uri?.let { loader.cachedPreview(it)?.asImageBitmap() }, uri) {
        value = uri?.let { loader.loadPreview(it)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .aspectRatio(1f)
            .pressScale(interaction, 0.92f)
            .jellyPop(selected)
            .wfSurface(Wf.Well, shape, if (selected) 8.dp else 3.dp)
            .clip(shape)
            .border(if (selected) BorderStroke(3.dp, Wf.AccentInk) else BorderStroke(0.dp, Color.Transparent), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (uri == null) "${number}번 칸에 사진 추가" else "${number}번 사진" }
            .testTag("photo_tile_$number"),
        contentAlignment = Alignment.Center,
    ) {
        val img: ImageBitmap? = image
        if (uri != null && img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else if (uri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Wf.TextDim)
                Text("추가", style = MaterialTheme.typography.labelSmall, color = Wf.TextDim)
            }
        }
        Box(
            Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp)
                .background(if (selected) Wf.Accent else Color.Black.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, color = if (selected) Wf.OnAccent else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleSection(style: CollageStyle, onChange: (CollageStyle, Boolean) -> Unit, onDragEnd: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("빠른 스타일", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StylePresets.all.forEach { p ->
                FilterChip(
                    selected = StylePresets.matches(p, style),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onChange(p.style.copy(backgroundColor = style.backgroundColor), true)
                    },
                    label = { Text(p.label) },
                    shape = Wf.PillShape,
                    colors = pillChipColors(),
                    border = null,
                    modifier = Modifier.jellyChip(StylePresets.matches(p, style)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        StyleSlider("사진 사이 간격", style.spacing, CollageStyle.MAX_SPACING, "spacing",
            { onChange(style.copy(spacing = it), false) }, onDragEnd)
        StyleSlider("바깥 테두리", style.margin, CollageStyle.MAX_MARGIN, "margin",
            { onChange(style.copy(margin = it), false) }, onDragEnd)
        StyleSlider("모서리 둥글게", style.cornerRadius, CollageStyle.MAX_CORNER, "corner",
            { onChange(style.copy(cornerRadius = it), false) }, onDragEnd)
        Spacer(Modifier.height(8.dp))
        Text("배경색 (간격·테두리 색)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StylePresets.backgrounds.forEach { argb ->
                val color = Color(argb)
                val selected = style.backgroundColor == argb
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(42.dp)
                        .pressScale(interaction, 0.85f)
                        .border(if (selected) 2.5.dp else 0.dp, if (selected) Wf.AccentInk else Color.Transparent, CircleShape)
                        .padding(if (selected) 5.dp else 0.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Wf.Text.copy(alpha = 0.12f), CircleShape)
                        .clickable(interactionSource = interaction, indication = null) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onChange(style.copy(backgroundColor = argb), true)
                        }
                        .semantics { contentDescription = "배경색 #" + argb.toString(16).uppercase() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp),
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleSlider(
    label: String,
    value: Float,
    max: Float,
    tag: String,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                value.toInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Wf.AccentInk,
                modifier = Modifier.background(Wf.Well, Wf.PillShape).padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it.toInt().toFloat()) },
            onValueChangeFinished = onFinished,
            valueRange = 0f..max,
            modifier = Modifier.testTag("slider_$tag"),
        )
    }
}

@Composable
private fun SizeSection(state: CollageState, onSelect: (String) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 240.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().testTag("preset_grid"),
    ) {
        Purposes.groups.forEach { group ->
            item(span = { GridItemSpan(maxLineSpan) }, key = group.title) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(group.purposes, key = { it.presetId }) { p ->
                PurposeRow(p, selected = p.presetId == state.presetId, onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(p.presetId)
                })
            }
        }
    }
}
