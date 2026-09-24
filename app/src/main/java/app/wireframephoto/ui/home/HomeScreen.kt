package app.wireframephoto.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.wireframephoto.ui.Purpose
import app.wireframephoto.ui.Purposes
import app.wireframephoto.ui.components.BrandMark
import app.wireframephoto.ui.components.FoldHero
import app.wireframephoto.ui.components.PurposeCard
import app.wireframephoto.ui.theme.Wf

private val Steps = listOf("쓸 곳 고르기", "사진 고르기", "배치 고르고 저장")

/**
 * Entry screen. A living Fold hero sets the mood; one question — where will the collage be
 * used? — fixes the canvas size, opens the photo picker, and the grid is chosen in the editor.
 */
@Composable
fun HomeScreen(onPurpose: (Purpose) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = 40.dp)) {
                    Column(
                        Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Brand()
                        Spacer(Modifier.height(20.dp))
                        FoldHero(height = 260.dp, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(24.dp))
                        Headline()
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
                            Brand()
                            Spacer(Modifier.height(8.dp))
                            FoldHero(height = 210.dp, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(18.dp))
                            Headline()
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

private fun LazyGridScope.purposeItems(onPurpose: (Purpose) -> Unit, previewHeight: Dp) {
    val fold = Purposes.featured.filter { it.deviceFrame }
    val social = Purposes.featured.filterNot { it.deviceFrame }
    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("갤럭시 Z 폴드 8 배경화면") }
    items(fold, key = { it.presetId }) { PurposeCard(it, { onPurpose(it) }, previewHeight) }
    item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("SNS · 공유용") }
    items(social, key = { it.presetId }) { PurposeCard(it, { onPurpose(it) }, previewHeight) }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark(Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("와이어프레임 포토", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Headline() {
    Column {
        Text(
            buildAnnotatedString {
                append("어디에 쓸\n")
                withStyle(SpanStyle(color = Wf.Lime)) { append("콜라주") }
                append("인가요?")
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
                    .border(1.dp, MaterialTheme.colorScheme.outline, Wf.PillShape)
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).background(if (i == 0) Wf.Lime else Wf.Steel, Wf.PillShape), contentAlignment = Alignment.Center) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == 0) Wf.OnLime else Wf.Paper,
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
        "갤러리에서 사진을 공유 → 와이어프레임 포토로 보내도 시작돼요.\n4:5, 폴드 8 Ultra 크기는 편집 화면의 '크기'에서 고를 수 있어요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}
