package app.wireframephoto

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wireframephoto.core.Templates
import app.wireframephoto.ui.EditorViewModel
import app.wireframephoto.ui.MainActivity
import app.wireframephoto.ui.editor.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun viewModel() = ViewModelProvider(rule.activity)[EditorViewModel::class.java]

    private fun photo(name: String, color: Int): Uri {
        val f = File(rule.activity.cacheDir, name)
        Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            .let { b -> f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return Uri.fromFile(f)
    }

    /** The app picks the layout from its content area, so ask the UI which one it rendered. */
    private fun renderedLayout(): String =
        listOf("layout_Compact", "layout_SidePanel", "layout_Tabletop").single {
            rule.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty()
        }

    private fun setGuideSeen(seen: Boolean) {
        rule.activity.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putBoolean("guide_seen", seen).commit()
    }

    private fun startEditor(vararg colors: Int, preset: String = "fold8_main") {
        val uris = colors.mapIndexed { i, c -> photo("p$i.png", c) }
        rule.runOnUiThread { viewModel().startNew(uris, preset) }
        rule.waitForIdle()
    }

    @Test
    fun home_asksWhatToMake_withPurposeCards() {
        rule.onNodeWithText("콜라주인가요?", substring = true).assertExists()
        rule.onNodeWithTag("purpose_fold8_cover").assertExists()
        rule.onNodeWithTag("purpose_fold8_main").assertExists()
    }

    @Test
    fun editor_usesLayoutMatchingWindowSize() {
        setGuideSeen(true)
        startEditor()
        val config = rule.activity.resources.configuration
        val layout = renderedLayout()
        // Narrow windows (cover screen) must never get the side panel.
        if (config.screenWidthDp < 600) assertEquals("layout_Compact", layout)
        // Wide landscape windows (unfolded main screen) must get it.
        if (config.screenWidthDp >= 840 && config.screenWidthDp > config.screenHeightDp) assertEquals("layout_SidePanel", layout)
        // Every tool is reachable in either layout.
        Tool.entries.forEach { rule.onNodeWithTag("tool_${it.name}").assertExists() }
        rule.onNodeWithTag("collage_canvas").assertExists()
        rule.onNodeWithText("메인 화면 배경화면").assertExists()
    }

    @Test
    fun guide_showsOnce_andCanBeReopened() {
        setGuideSeen(false)
        startEditor(0xFFFF0000.toInt())
        rule.onNodeWithTag("guide").assertExists()
        rule.onNodeWithTag("guide_ok").performClick()
        rule.onNodeWithTag("guide").assertDoesNotExist()

        // Next collage: already seen, so no guide until "?" is pressed.
        rule.runOnUiThread { viewModel().close() }
        startEditor(0xFFFF0000.toInt())
        rule.onNodeWithTag("guide").assertDoesNotExist()
        rule.onNodeWithTag("help").performClick()
        rule.onNodeWithTag("guide").assertExists()
    }

    @Test
    fun templateChange_isUndoable() {
        setGuideSeen(true)
        startEditor(0xFFFF0000.toInt(), 0xFF0000FF.toInt())
        val before = viewModel().state.value.templateId
        rule.onNodeWithTag("undo").assertIsNotEnabled()

        val other = Templates.forCount(2).first { it.id != before }
        // Folded layout starts with tools closed; the unfolded side panel shows 배치 by default.
        if (renderedLayout() == "layout_Compact") rule.onNodeWithTag("tool_Layout").performClick()
        rule.onNodeWithTag("template_${other.id}").performClick()
        rule.waitForIdle()
        assertEquals(other.id, viewModel().state.value.templateId)

        rule.onNodeWithTag("undo").assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(before, viewModel().state.value.templateId)
    }

    @Test
    fun sizeTool_changesPurpose() {
        setGuideSeen(true)
        startEditor(0xFFFF0000.toInt())
        rule.onNodeWithTag("tool_Size").performClick()
        rule.onNodeWithTag("preset_grid").performScrollToNode(hasTestTag("preset_sq"))
        rule.onNodeWithTag("preset_sq").performClick()
        rule.waitForIdle()
        assertEquals("sq", viewModel().state.value.presetId)
        // Top bar title + the selected card.
        assertEquals(2, rule.onAllNodesWithText("정사각형").fetchSemanticsNodes().size)
    }

    @Test
    fun photosTool_showsNumberedTiles_andEmptyTileForMissingPhoto() {
        setGuideSeen(true)
        startEditor(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        rule.runOnUiThread { viewModel().selectTemplate(Templates.defaultFor(3).id) }
        rule.onNodeWithTag("tool_Photos").performClick()
        rule.onNodeWithTag("photo_tile_1").assertExists()
        rule.onNodeWithTag("photo_tile_3").assertExists()
        rule.onNodeWithTag("add_photos").assertIsEnabled()
    }

    @Test
    fun compactBottomBar_togglesPanel() {
        setGuideSeen(true)
        startEditor(0xFFFF0000.toInt())
        assumeTrue("folded/cover-screen layout only", renderedLayout() == "layout_Compact")
        // Starts closed so the collage gets the whole screen first.
        rule.onNodeWithTag("panel_Layout").assertDoesNotExist()
        rule.onNodeWithTag("tool_Style").performClick()
        rule.onNodeWithTag("panel_Style").assertExists()
        // Tapping the open tool again collapses the panel so the canvas gets the whole screen.
        rule.onNodeWithTag("tool_Style").performClick()
        rule.onNodeWithTag("panel_Style").assertDoesNotExist()
    }

    @Test
    fun shuffle_changesLayout_andIsUndoable() {
        setGuideSeen(true)
        startEditor(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
        val before = viewModel().state.value
        rule.onNodeWithTag("shuffle").performClick()
        rule.waitForIdle()
        val after = viewModel().state.value
        assertNotEquals(before.templateId, after.templateId)
        rule.onNodeWithTag("undo").performClick()
        rule.waitForIdle()
        assertEquals(before, viewModel().state.value)
    }

    @Test
    fun stateSurvivesActivityRecreation() {
        setGuideSeen(true)
        startEditor(0xFF00FF00.toInt())
        val before = viewModel().state.value
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertEquals(before, viewModel().state.value)
        rule.onNodeWithTag("collage_canvas").assertExists()
    }
}
