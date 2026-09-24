package app.wireframephoto.ui

import app.wireframephoto.core.AspectPresets

/**
 * User-facing names for output presets. People think "cover-screen wallpaper", not "10:16",
 * so every screen talks in purposes and shows the ratio only as secondary detail.
 */
data class Purpose(
    val presetId: String,
    val title: String,
    val subtitle: String,
    /** Wallpaper purposes are previewed inside a device bezel. */
    val deviceFrame: Boolean = false,
) {
    val preset get() = AspectPresets.byId(presetId)
}

data class PurposeGroup(val title: String, val purposes: List<Purpose>)

object Purposes {
    val FoldCover = Purpose("fold8_cover", "커버 화면 배경화면", "접었을 때 바깥 화면", deviceFrame = true)
    val FoldMain = Purpose("fold8_main", "메인 화면 배경화면", "펼쳤을 때 안쪽 화면", deviceFrame = true)

    val groups = listOf(
        PurposeGroup("갤럭시 Z 폴드 8", listOf(FoldCover, FoldMain)),
        PurposeGroup(
            "SNS · 공유",
            listOf(
                Purpose("p34", "인스타그램 피드", "세로 3:4"),
                Purpose("p45", "인스타그램 세로", "세로 4:5"),
                Purpose("p916", "스토리 · 릴스", "세로 9:16"),
                Purpose("sq", "정사각형", "1:1"),
                Purpose("l169", "가로 사진", "가로 16:9"),
            ),
        ),
        PurposeGroup(
            "갤럭시 Z 폴드 8 Ultra",
            listOf(
                Purpose("fold8u_cover", "Ultra 커버 화면", "접었을 때 바깥 화면", deviceFrame = true),
                Purpose("fold8u_main", "Ultra 메인 화면", "펼쳤을 때 안쪽 화면", deviceFrame = true),
            ),
        ),
    )

    val all = groups.flatMap { it.purposes }

    /** Shown on the home screen: the Fold screens first, then the most common social formats. */
    val featured = listOf(FoldCover, FoldMain) + listOf("p34", "p916", "sq", "l169").map(::of)

    fun of(presetId: String): Purpose = all.firstOrNull { it.presetId == presetId } ?: FoldMain
}
