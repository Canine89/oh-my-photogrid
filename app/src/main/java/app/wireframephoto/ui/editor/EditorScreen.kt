package app.wireframephoto.ui.editor

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Pinch
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wireframephoto.core.CollageState
import app.wireframephoto.core.Templates
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.ui.EditorViewModel
import app.wireframephoto.ui.ImagesOnly
import app.wireframephoto.ui.Purposes
import app.wireframephoto.ui.persistReadAccess
import app.wireframephoto.ui.theme.Wf

/** Which arrangement the editor uses for the current window. Exposed for tests and screenshots. */
enum class EditorLayout { Compact, SidePanel, Tabletop }

private const val PREFS = "ui"
private const val KEY_GUIDE_SEEN = "guide_seen"
private val ToolbarHeight = 76.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCell.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val export by viewModel.export.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val purpose = Purposes.of(state.presetId)

    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }
    var replaceTarget by rememberSaveable { mutableStateOf<Int?>(null) }
    // Unfolded: which tab is showing. Folded: which panel is open above the toolbar (null = closed).
    var tab by rememberSaveable { mutableStateOf(Tool.Layout) }
    // Folded: start closed so the first thing you see is your collage, big (time-to-first-delight).
    var openTool by rememberSaveable { mutableStateOf<Tool?>(null) }
    // True while a finger is zooming/panning a photo: chrome steps back so the canvas has the stage.
    var interacting by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var showGuide by rememberSaveable { mutableStateOf(!prefs.getBoolean(KEY_GUIDE_SEEN, false)) }
    val dismissGuide = {
        prefs.edit { putBoolean(KEY_GUIDE_SEEN, true) }
        showGuide = false
    }

    val pickOne = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        val cell = replaceTarget
        if (uri != null && cell != null) {
            context.persistReadAccess(listOf(uri))
            viewModel.replacePhoto(cell, uri)
            viewModel.select(cell)
        }
        replaceTarget = null
    }
    val pickMany = rememberLauncherForActivityResult(PickMultipleVisualMedia(Templates.MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) {
            context.persistReadAccess(uris)
            viewModel.addPhotos(uris)
        }
    }
    val pickForCell: (Int) -> Unit = { cell ->
        replaceTarget = cell
        pickOne.launch(ImagesOnly)
    }

    BackHandler {
        if (state.photoCount > 0) confirmExit = true else viewModel.close()
    }

    val actions = remember(viewModel) {
        CanvasActions(
            onTapCell = { viewModel.select(it) },
            onTapEmptyCell = { cell -> viewModel.select(cell); pickForCell(cell) },
            onTapOutside = { viewModel.select(null) },
            onDoubleTapCell = { viewModel.resetTransform(it) },
            onGestureStart = { interacting = true; viewModel.beginGesture() },
            onTransform = viewModel::updateTransform,
            onGestureEnd = { interacting = false; viewModel.endGesture() },
            onSwap = viewModel::swap,
            onDropPhotos = { cell, uris ->
                if (cell >= 0) {
                    viewModel.replacePhoto(cell, uris.first())
                    if (uris.size > 1) viewModel.addPhotos(uris.drop(1))
                } else {
                    viewModel.addPhotos(uris)
                }
            },
        )
    }
    val toolCallbacks = remember(viewModel) {
        ToolCallbacks(
            onSelectTemplate = viewModel::selectTemplate,
            onSelectPreset = viewModel::selectPreset,
            onStyleChange = viewModel::updateStyle,
            onStyleDragEnd = viewModel::endGesture,
            onSelectCell = { viewModel.select(it) },
            onPickForCell = pickForCell,
            onAddPhotos = { pickMany.launch(ImagesOnly) },
            onShuffle = viewModel::shuffle,
        )
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(purpose.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "사진 ${state.photoCount}장 · ${state.preset.width}×${state.preset.height}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (state.photoCount > 0) confirmExit = true else viewModel.close() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "처음 화면으로")
                    }
                },
                actions = {
                    IconButton(onClick = { showGuide = true }, modifier = Modifier.testTag("help")) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "사용법")
                    }
                    IconButton(onClick = viewModel::undo, enabled = canUndo, modifier = Modifier.testTag("undo")) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "실행 취소")
                    }
                    IconButton(onClick = viewModel::redo, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "다시 실행")
                    }
                    Button(
                        onClick = { showExportOptions = true },
                        enabled = state.photoCount > 0,
                        shape = Wf.PillShape,
                        modifier = Modifier.padding(start = 4.dp, end = 10.dp).testTag("save"),
                    ) { Text("저장", fontWeight = FontWeight.ExtraBold) }
                },
            )
        },
    ) { padding ->
        val posture = currentWindowAdaptiveInfoV2().windowPosture
        val hinge = posture.hingeList.firstOrNull()
        var topInWindow by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        BoxWithConstraints(
            Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .onGloballyPositioned { topInWindow = it.positionInWindow().y },
        ) {
            val maxW = maxWidth
            val maxH = maxHeight
            val layout = when {
                posture.isTabletop && hinge != null -> EditorLayout.Tabletop
                maxWidth >= 600.dp && maxWidth > maxHeight -> EditorLayout.SidePanel
                else -> EditorLayout.Compact
            }
            val activeTool = if (layout == EditorLayout.Compact) openTool else tab
            val canvasArea: @Composable (Modifier, Boolean) -> Unit = { m, floatingShuffle ->
                CanvasArea(
                    state = state,
                    selected = selected,
                    viewModel = viewModel,
                    actions = actions,
                    onReplace = pickForCell,
                    deviceFrame = purpose.deviceFrame,
                    showNumbers = activeTool == Tool.Photos,
                    floatingShuffle = floatingShuffle,
                    interacting = interacting,
                    modifier = m,
                )
            }
            val sidePanel: @Composable (Modifier) -> Unit = { m ->
                ToolSidePanel(tab, { tab = it }, state, selected, viewModel.loader, toolCallbacks, m)
            }
            when (layout) {
                EditorLayout.Tabletop -> {
                    // Flex mode: canvas above the hinge, controls on the lower half resting on the table.
                    val hingeTop = with(density) { (hinge!!.bounds.top - topInWindow).coerceAtLeast(0f).toDp() }
                    val hingeHeight = with(density) { hinge!!.bounds.height.toDp() }
                    Column(Modifier.fillMaxSize().testTag("layout_Tabletop")) {
                        canvasArea(Modifier.fillMaxWidth().height(hingeTop.coerceIn(160.dp, maxH - 160.dp)), true)
                        Spacer(Modifier.height(hingeHeight))
                        sidePanel(Modifier.fillMaxWidth().weight(1f).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }

                EditorLayout.SidePanel -> Row(
                    Modifier.fillMaxSize().navigationBarsPadding().padding(start = 4.dp, end = 12.dp, bottom = 12.dp)
                        .testTag("layout_SidePanel"),
                ) {
                    canvasArea(Modifier.weight(1f).fillMaxHeight(), true)
                    Spacer(Modifier.width(8.dp))
                    sidePanel(Modifier.width(sidePanelWidth(maxW)).fillMaxHeight())
                }

                EditorLayout.Compact -> Box(Modifier.fillMaxSize().testTag("layout_Compact")) {
                    val panelHeight = (maxH * 0.36f).coerceIn(190.dp, 260.dp)
                    val open = openTool
                    // The canvas shrinks smoothly so the floating sheet never covers the collage.
                    val reserve by animateDpAsState(
                        navBottom + ToolbarHeight + 10.dp + if (open != null) panelHeight + 8.dp else 0.dp,
                        Wf.snappy(),
                        label = "reserve",
                    )
                    canvasArea(Modifier.fillMaxSize().padding(bottom = reserve), false)
                    var lastTool by remember { mutableStateOf(open ?: Tool.Layout) }
                    if (open != null) lastTool = open
                    Column(
                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AnimatedVisibility(
                            visible = open != null,
                            enter = slideInVertically(Wf.bouncy()) { it / 3 } + fadeIn(),
                            exit = slideOutVertically(Wf.snappy()) { it / 3 } + fadeOut(),
                        ) {
                            CompactToolSheet(
                                tool = lastTool,
                                onClose = { openTool = null },
                                modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth().height(panelHeight),
                            ) { ToolSection(lastTool, state, selected, viewModel.loader, toolCallbacks) }
                        }
                        Spacer(Modifier.height(8.dp))
                        FloatingToolBar(
                            openTool = openTool,
                            onToggle = { t -> openTool = if (openTool == t) null else t },
                            onShuffle = viewModel::shuffle,
                            dimmed = interacting,
                        )
                    }
                }
            }
        }
    }

    // Full-screen so it always fits, even on the cover screen where the canvas area is small.
    val configuration = LocalConfiguration.current
    val wideWindow = configuration.screenWidthDp >= 600 && configuration.screenWidthDp > configuration.screenHeightDp
    AnimatedVisibility(visible = showGuide, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Wf.Ink.copy(alpha = 0.72f))
                // Swallow touches so the editor underneath can't be used, without merging semantics
                // (a clickable scrim would hide the card's content from TalkBack).
                .pointerInput(Unit) { detectTapGestures { } }
                .safeDrawingPadding().padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            GuideCard(toolsWhere = if (wideWindow) "오른쪽" else "아래", onDismiss = dismissGuide)
        }
    }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            containerColor = Wf.Graphite,
            title = { Text("처음 화면으로 갈까요?") },
            text = { Text("저장하지 않은 콜라주는 사라져요.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; viewModel.close() }) { Text("나가기") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("계속 편집") } },
        )
    }

    ExportDialogs(
        state = state,
        loader = viewModel.loader,
        showOptions = showExportOptions,
        status = export,
        onDismissOptions = { showExportOptions = false },
        onExport = { scale, format ->
            showExportOptions = false
            viewModel.export(scale, format)
        },
        onDismissStatus = viewModel::dismissExport,
    )
}

private fun sidePanelWidth(total: Dp): Dp = (total * 0.38f).coerceIn(340.dp, 460.dp)

/** Folded screen: the open tool as a floating rounded sheet above the toolbar. */
@Composable
private fun CompactToolSheet(tool: Tool, onClose: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.shadow(20.dp, Wf.SheetShape).testTag("panel_${tool.name}"),
        shape = Wf.SheetShape,
        color = Wf.Graphite,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tool.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ExpandMore, contentDescription = "도구 닫기") }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

/** Soft light behind the canvas tinted by the photos themselves (like "ambient mode" in video apps). */
@Composable
private fun AmbientGlow(state: CollageState, loader: BitmapLoader, modifier: Modifier) {
    val uris = state.slots.take(state.template.count).mapNotNull { it.uri }
    val target by produceState(Wf.Lime, uris) {
        val colors = uris.mapNotNull { loader.averageColor(it) }
        if (colors.isEmpty()) return@produceState
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(ColorUtils.blendARGB(colors.first(), colors.last(), 0.5f), hsl)
        // Averages of photos are muddy; lift saturation and lightness so the glow reads as color.
        hsl[1] = hsl[1].coerceAtLeast(0.55f)
        hsl[2] = hsl[2].coerceIn(0.5f, 0.65f)
        value = Color(ColorUtils.HSLToColor(hsl))
    }
    val glow by animateColorAsState(target, Wf.snappy(), label = "glow")
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                listOf(glow.copy(alpha = 0.32f), glow.copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = size.maxDimension * 0.62f,
            ),
        )
    }
}

@Composable
private fun CanvasArea(
    state: CollageState,
    selected: Int?,
    viewModel: EditorViewModel,
    actions: CanvasActions,
    onReplace: (Int) -> Unit,
    deviceFrame: Boolean,
    showNumbers: Boolean,
    floatingShuffle: Boolean,
    interacting: Boolean,
    modifier: Modifier,
) {
    val cell = selected?.takeIf { it < state.template.count }
    val bottomBar = cell != null || floatingShuffle
    Box(modifier) {
        AmbientGlow(state, viewModel.loader, Modifier.fillMaxSize())
        CollageCanvas(
            state = state,
            selectedCell = selected,
            loader = viewModel.loader,
            actions = actions,
            deviceFrame = deviceFrame,
            showNumbers = showNumbers,
            modifier = Modifier.fillMaxSize().padding(18.dp).padding(bottom = if (bottomBar) 60.dp else 0.dp),
        )
        AnimatedVisibility(
            visible = cell != null,
            enter = scaleIn(Wf.bouncy(), initialScale = 0.8f) + fadeIn(),
            exit = scaleOut(Wf.snappy(), targetScale = 0.9f) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        ) {
            val c = cell ?: 0
            CellActionBar(
                number = c + 1,
                hasPhoto = state.slot(c).uri != null,
                onReplace = { onReplace(c) },
                onReset = { viewModel.resetTransform(c) },
                onClear = { viewModel.clearPhoto(c) },
                onDismiss = { viewModel.select(null) },
            )
        }
        if (floatingShuffle && cell == null) {
            ShufflePill(
                onShuffle = viewModel::shuffle,
                dimmed = interacting,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun CellActionBar(
    number: Int,
    hasPhoto: Boolean,
    onReplace: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.shadow(16.dp, Wf.PillShape).testTag("cell_actions"),
        shape = Wf.PillShape,
        color = Wf.Steel,
        border = BorderStroke(1.dp, Wf.Fog.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).background(Wf.Lime, CircleShape), contentAlignment = Alignment.Center) {
                Text("$number", style = MaterialTheme.typography.labelLarge, color = Wf.OnLime)
            }
            TextButton(onClick = onReplace) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (hasPhoto) "바꾸기" else "사진 넣기")
            }
            if (hasPhoto) {
                TextButton(onClick = onReset) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("원래대로")
                }
                TextButton(onClick = onClear) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("빼기")
                }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "선택 해제") }
        }
    }
}

/** First-run (and "?" button) explanation of the gestures, which are otherwise invisible. */
@Composable
private fun GuideCard(toolsWhere: String, onDismiss: () -> Unit) {
    Surface(
        Modifier.widthIn(max = 440.dp).shadow(24.dp, Wf.SheetShape).testTag("guide"),
        shape = Wf.SheetShape,
        color = Wf.Graphite,
        border = BorderStroke(1.dp, Wf.Fog),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("이렇게 편집해요", style = MaterialTheme.typography.titleLarge)
            GuideRow(Icons.Outlined.TouchApp, "칸을 누르면 사진을 바꾸거나 뺄 수 있어요. 빈 칸은 누르면 바로 사진을 골라요.")
            GuideRow(Icons.Outlined.Pinch, "두 손가락으로 벌리면 확대, 한 손가락으로 끌면 보이는 부분을 옮겨요.")
            GuideRow(Icons.Outlined.OpenWith, "꾹 누른 채 다른 칸으로 끌어 놓으면 두 사진 자리가 바뀌어요.")
            GuideRow(Icons.Outlined.Tune, "$toolsWhere 메뉴에서 배치·사진·꾸미기·크기를 바꾸고, 주사위로 섞어 볼 수도 있어요. 다 되면 오른쪽 위 '저장'!")
            Button(
                onClick = onDismiss,
                shape = Wf.PillShape,
                modifier = Modifier.align(Alignment.End).testTag("guide_ok"),
            ) { Text("알겠어요", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun GuideRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).background(Wf.Slate, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Wf.Lime, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    }
}
