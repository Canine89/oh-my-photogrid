package app.wireframephoto.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.wireframephoto.data.AvailableUpdate
import app.wireframephoto.data.UpdatePhase
import app.wireframephoto.ui.Purpose
import app.wireframephoto.ui.Purposes
import app.wireframephoto.ui.components.BrandMark
import app.wireframephoto.ui.components.FoldHero
import app.wireframephoto.ui.components.JellyBackdrop
import app.wireframephoto.ui.components.PurposeCard
import app.wireframephoto.ui.components.jellyButton
import app.wireframephoto.ui.components.jellyButtonColors
import app.wireframephoto.ui.components.jellyChip
import app.wireframephoto.ui.components.jellyEnter
import app.wireframephoto.ui.components.wfSurface
import app.wireframephoto.ui.theme.AppTheme
import app.wireframephoto.ui.theme.LocalWfPalette
import app.wireframephoto.ui.theme.Wf

private val Steps = listOf("쓸 곳 고르기", "사진 고르기", "배치 고르고 저장")

/**
 * Entry screen. A living Fold hero sets the mood; one question — where will the collage be
 * used? — fixes the canvas size, opens the photo picker, and the grid is chosen in the editor.
 */
@Composable
fun HomeScreen(
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    update: AvailableUpdate?,
    updatePhase: UpdatePhase,
    onUpdate: (AvailableUpdate) -> Unit,
    onOpenReleasePage: (AvailableUpdate) -> Unit,
    onDismissUpdate: (AvailableUpdate) -> Unit,
    onPurpose: (Purpose) -> Unit,
) {
    val jelly = LocalWfPalette.current.jelly
    Box(Modifier.fillMaxSize()) {
    JellyBackdrop(Modifier.matchParentSize())
    Surface(Modifier.fillMaxSize(), color = if (jelly) Color.Transparent else MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = 40.dp)) {
                    Column(
                        Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Brand(theme, onThemeChange)
                        update?.let { UpdateBanner(it, updatePhase, onUpdate, onOpenReleasePage, onDismissUpdate, Modifier.padding(top = 14.dp)) }
                        Spacer(Modifier.height(20.dp))
                        FoldHero(height = 260.dp, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(24.dp))
                        Headline(theme)
                        Spacer(Modifier.height(18.dp))
                        StepChips()
                    }
                    Spacer(Modifier.width(40.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        modifier = Modifier.weight(1.25f).fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        purposeItems(onPurpose, previewHeight = 128.dp)
                        item(span = { GridItemSpan(maxLineSpan) }) { Footnote() }
                    }
                }
            } else {
                // Cover screen: one scrolling column, two cards per row within thumb reach.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Brand(theme, onThemeChange)
                            update?.let { UpdateBanner(it, updatePhase, onUpdate, onOpenReleasePage, onDismissUpdate, Modifier.padding(top = 12.dp)) }
                            Spacer(Modifier.height(8.dp))
                            FoldHero(height = 210.dp, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(18.dp))
                            Headline(theme)
                            Spacer(Modifier.height(14.dp))
                            StepChips()
                        }
                    }
                    purposeItems(onPurpose, previewHeight = 104.dp)
                    item(span = { GridItemSpan(maxLineSpan) }) { Footnote() }
                }
            }
        }
    }
    }
}

private fun LazyGridScope.purposeItems(onPurpose: (Purpose) -> Unit, previewHeight: Dp) {
    val fold = Purposes.featured.filter { it.deviceFrame }
    val social = Purposes.featured.filterNot { it.deviceFrame }
    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("갤럭시 Z 폴드 8 배경화면") }
    itemsIndexed(fold, key = { _, p -> p.presetId }) { i, p ->
        PurposeCard(p, { onPurpose(p) }, previewHeight, Modifier.jellyEnter(i))
    }
    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("SNS · 공유용") }
    itemsIndexed(social, key = { _, p -> p.presetId }) { i, p ->
        PurposeCard(p, { onPurpose(p) }, previewHeight, Modifier.jellyEnter(fold.size + i))
    }
}

@Composable
private fun Brand(theme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark(Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("oh-my-photogrid", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        ThemeSwitch(theme, onThemeChange)
    }
}

/**
 * New release on GitHub: one tap downloads it inside the app and opens the system's
 * "update this app?" screen. If that fails, the release page is offered instead.
 */
@Composable
private fun UpdateBanner(
    update: AvailableUpdate,
    phase: UpdatePhase,
    onUpdate: (AvailableUpdate) -> Unit,
    onOpenReleasePage: (AvailableUpdate) -> Unit,
    onDismiss: (AvailableUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val jelly = LocalWfPalette.current.jelly
    Surface(
        shape = Wf.CardShape,
        color = if (jelly) Color.Transparent else Wf.Card,
        border = if (jelly) null else BorderStroke(1.dp, Wf.AccentInk.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().wfSurface(Wf.Card, Wf.CardShape, 10.dp).testTag("update_banner"),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).wfSurface(Wf.Accent, CircleShape, 3.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = Wf.OnAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("새 버전(${update.version})이 나왔어요", style = MaterialTheme.typography.titleSmall)
                Text(
                    when (phase) {
                        UpdatePhase.Idle -> "받아서 바로 업데이트해요"
                        is UpdatePhase.Downloading -> "받는 중… ${(phase.progress * 100).toInt()}%"
                        UpdatePhase.Installing -> "설치 화면에서 확인해 주세요"
                        UpdatePhase.Failed -> "설치하지 못했어요. 페이지에서 받아 주세요"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (phase is UpdatePhase.Downloading) {
                    LinearProgressIndicator(
                        progress = { phase.progress },
                        color = Wf.AccentInk,
                        trackColor = Wf.Well,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val busy = phase is UpdatePhase.Downloading || phase == UpdatePhase.Installing
            Button(
                onClick = { if (phase == UpdatePhase.Failed) onOpenReleasePage(update) else onUpdate(update) },
                enabled = !busy,
                shape = Wf.PillShape,
                colors = jellyButtonColors(Wf.Accent, Wf.OnAccent),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.jellyButton(Wf.Accent, enabled = !busy).testTag("update_get"),
            ) { Text(if (phase == UpdatePhase.Failed) "페이지 열기" else "업데이트") }
            IconButton(onClick = { onDismiss(update) }, enabled = !busy, modifier = Modifier.testTag("update_later")) {
                Icon(Icons.Outlined.Close, contentDescription = "나중에", tint = Wf.TextDim)
            }
        }
    }
}

/** Two-way pill switch between the looks. Each option is a radio button for TalkBack. */
@Composable
private fun ThemeSwitch(theme: AppTheme, onChange: (AppTheme) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val jelly = LocalWfPalette.current.jelly
    Row(
        Modifier.background(Wf.Well, Wf.PillShape).padding(3.dp).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTheme.entries.forEach { option ->
            val selected = option == theme
            val container by animateColorAsState(if (selected) Wf.Accent else Wf.Well, Wf.snappy(), label = "themeBg")
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .then(if (jelly) Modifier.jellyChip(selected) else Modifier.clip(Wf.PillShape).background(container))
                    .clip(Wf.PillShape)
                    .selectable(selected, role = Role.RadioButton) {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onChange(option)
                        }
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("theme_${option.name}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Wf.OnAccent else Wf.TextDim,
                )
            }
        }
    }
}

@Composable
private fun Headline(theme: AppTheme) {
    Column {
        Text(
            buildAnnotatedString {
                if (theme == AppTheme.Album) {
                    append("우리 가족 사진,\n어디에 ")
                    withStyle(SpanStyle(color = Wf.AccentInk)) { append("담아 볼까요") }
                    append("?")
                } else {
                    append("어디에 쓸\n")
                    withStyle(SpanStyle(color = Wf.AccentInk)) { append("콜라주") }
                    append("인가요?")
                }
            },
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "쓸 곳을 고르면 크기가 딱 맞춰지고, 바로 사진을 고를 수 있어요. 사진 배치는 다음 화면에서 골라요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepChips() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Steps.forEachIndexed { i, s ->
            Row(
                Modifier
                    .then(
                        if (LocalWfPalette.current.jelly) Modifier.wfSurface(Wf.Card, Wf.PillShape, 4.dp)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, Wf.PillShape),
                    )
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).wfSurface(if (i == 0) Wf.Accent else Wf.Raised, Wf.PillShape, 2.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == 0) Wf.OnAccent else Wf.Text,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(s, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun Footnote() {
    Text(
        "갤러리에서 사진을 공유 → oh-my-photogrid로 보내도 시작돼요.\n4:5, 폴드 8 Ultra 크기는 편집 화면의 '크기'에서 고를 수 있어요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}
