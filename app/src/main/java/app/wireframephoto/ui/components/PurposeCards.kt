package app.wireframephoto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.wireframephoto.core.CollageGeometry
import app.wireframephoto.ui.Purpose
import app.wireframephoto.ui.theme.Wf
import kotlin.math.min

val SampleColors = listOf(Color(0xFF7FD1FF), Color(0xFFFFC56E), Color(0xFFFF8A8A), Color(0xFF9EE6B8), Color(0xFFC9B6FF))

/**
 * Shape of the output only — a phone screen for wallpapers, a frame for social formats — with a
 * single photo glyph. Deliberately no grid: layouts are chosen once, in the editor's 배치 tool.
 */
@Composable
fun PurposePreview(purpose: Purpose, modifier: Modifier = Modifier) {
    val aspect = purpose.preset.aspect
    val bezelColor = Wf.TextDim
    val screenColor = Wf.Raised
    val glyphColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val bezel = if (purpose.deviceFrame) 3.dp.toPx() else 0f
        val fit = CollageGeometry.fit(aspect, size.width - 2 * bezel, size.height - 2 * bezel)
        val left = fit.left + bezel
        val top = fit.top + bezel
        if (purpose.deviceFrame) {
            drawRoundRect(
                bezelColor,
                topLeft = Offset(left - bezel, top - bezel),
                size = Size(fit.width + 2 * bezel, fit.height + 2 * bezel),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
        }
        drawRoundRect(screenColor, Offset(left, top), Size(fit.width, fit.height), CornerRadius(4.dp.toPx()))
        if (!purpose.deviceFrame) {
            drawRoundRect(outline, Offset(left, top), Size(fit.width, fit.height), CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        }
        // Photo glyph: sun + mountains, sized to the shorter side.
        val g = min(fit.width, fit.height) * 0.5f
        val cx = left + fit.width / 2f
        val cy = top + fit.height / 2f
        drawCircle(glyphColor, g * 0.12f, Offset(cx + g * 0.22f, cy - g * 0.22f))
        drawPath(
            Path().apply {
                moveTo(cx - g * 0.5f, cy + g * 0.35f)
                lineTo(cx - g * 0.15f, cy - g * 0.1f)
                lineTo(cx + g * 0.08f, cy + g * 0.15f)
                lineTo(cx + g * 0.22f, cy + g * 0.02f)
                lineTo(cx + g * 0.5f, cy + g * 0.35f)
                close()
            },
            glyphColor,
        )
        if (purpose.deviceFrame) drawCircle(bezelColor, 2.dp.toPx(), Offset(cx, top + 5.dp.toPx()))
    }
}

/** Large card used on the home screen: "where will you use it?" Squishes when pressed. */
@Composable
fun PurposeCard(purpose: Purpose, onClick: () -> Unit, previewHeight: Dp, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = Wf.CardShape,
        color = Wf.Card,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction).testTag("purpose_${purpose.presetId}"),
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier.fillMaxWidth().height(previewHeight).clip(RoundedCornerShape(20.dp))
                    .background(if (purpose.deviceFrame) Wf.GlowBrush else SolidColor(Wf.Well)),
                contentAlignment = Alignment.Center,
            ) {
                PurposePreview(purpose, Modifier.fillMaxSize().padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp)) {
                Text(purpose.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    purpose.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${purpose.preset.width}×${purpose.preset.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** Compact selectable row used in the editor's "크기" tool. */
@Composable
fun PurposeRow(purpose: Purpose, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Wf.Raised else Wf.Well,
        border = if (selected) BorderStroke(2.dp, primary) else null,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction).testTag("preset_${purpose.presetId}").semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PurposePreview(purpose, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(purpose.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${purpose.subtitle} · ${purpose.preset.width}×${purpose.preset.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = "선택됨", tint = primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** App mark: a three-cell wireframe in the theme accents, the same motif as the launcher icon. */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val (a, b, c) = listOf(Wf.Accent, Wf.Accent2, Wf.Accent3)
    Canvas(modifier) {
        val g = size.minDimension * 0.08f
        val r = CornerRadius(size.minDimension * 0.14f)
        val w = size.width
        val h = size.height
        drawRoundRect(a, Offset(0f, 0f), Size(w * 0.58f - g / 2, h), r)
        drawRoundRect(b, Offset(w * 0.58f + g / 2, 0f), Size(w * 0.42f - g / 2, h / 2 - g / 2), r)
        drawRoundRect(c, Offset(w * 0.58f + g / 2, h / 2 + g / 2), Size(w * 0.42f - g / 2, h / 2 - g / 2), r)
    }
}
