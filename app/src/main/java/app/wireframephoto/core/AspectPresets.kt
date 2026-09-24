package app.wireframephoto.core

enum class PresetGroup { FOLD8, FOLD8_ULTRA, SOCIAL }

/** Output canvas preset. [width]x[height] is the full-quality export size in pixels. */
data class AspectPreset(
    val id: String,
    val label: String,
    val group: PresetGroup,
    val width: Int,
    val height: Int,
    /** True when the preset matches a physical screen, so "set as wallpaper" is offered. */
    val isWallpaper: Boolean = false,
) {
    val aspect get() = width.toFloat() / height

    /** Export size at [scale] (e.g. 0.5 for a quick-share version), rounded to whole pixels. */
    fun sizeAt(scale: Float): Pair<Int, Int> =
        Math.round(width * scale).coerceAtLeast(1) to Math.round(height * scale).coerceAtLeast(1)
}

object AspectPresets {
    val all = listOf(
        AspectPreset("fold8_main", "메인 4:3", PresetGroup.FOLD8, 2448, 1848, isWallpaper = true),
        AspectPreset("fold8_cover", "커버 10:16", PresetGroup.FOLD8, 1248, 1972, isWallpaper = true),
        AspectPreset("fold8u_main", "메인 10:9", PresetGroup.FOLD8_ULTRA, 2504, 2256, isWallpaper = true),
        AspectPreset("fold8u_cover", "커버 21:9", PresetGroup.FOLD8_ULTRA, 1080, 2520, isWallpaper = true),
        AspectPreset("sq", "1:1", PresetGroup.SOCIAL, 2160, 2160),
        AspectPreset("p45", "4:5", PresetGroup.SOCIAL, 2160, 2700),
        AspectPreset("p34", "3:4", PresetGroup.SOCIAL, 2160, 2880),
        AspectPreset("p916", "9:16", PresetGroup.SOCIAL, 2160, 3840),
        AspectPreset("l169", "16:9", PresetGroup.SOCIAL, 3840, 2160),
    )

    val Default = all.first()

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: Default
}
