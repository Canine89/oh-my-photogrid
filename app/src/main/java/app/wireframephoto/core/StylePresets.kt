package app.wireframephoto.core

import kotlin.random.Random

data class StylePreset(val label: String, val style: CollageStyle)

/** Curated looks for one-tap styling and for Shuffle. Background colors are ARGB. */
object StylePresets {
    val all = listOf(
        StylePreset("꽉 채우기", CollageStyle(spacing = 0f, margin = 0f, cornerRadius = 0f)),
        StylePreset("얇은 테두리", CollageStyle(spacing = 12f, margin = 12f, cornerRadius = 0f)),
        StylePreset("넓은 여백", CollageStyle(spacing = 24f, margin = 40f, cornerRadius = 0f)),
        StylePreset("둥근 카드", CollageStyle(spacing = 20f, margin = 28f, cornerRadius = 36f)),
    )

    val backgrounds = listOf(
        0xFFFFFFFF, 0xFFF5F1EA, 0xFFE6E6E6, 0xFF2B2B2B, 0xFF000000, 0xFF1E2A3A,
        0xFF7FD1FF, 0xFFFFC56E, 0xFFFF8A8A, 0xFF9EE6B8, 0xFFC9B6FF, 0xFFFFB6D9,
    )

    fun matches(preset: StylePreset, style: CollageStyle) =
        style.spacing == preset.style.spacing && style.margin == preset.style.margin &&
            style.cornerRadius == preset.style.cornerRadius
}

/**
 * "Shuffle": same photos, a different layout, look and background — a small, safe surprise.
 * Always changes the layout when another one exists for this photo count, so every tap visibly does something.
 */
fun CollageState.shuffled(random: Random = Random.Default): CollageState {
    val count = template.count
    val layouts = Templates.forCount(count).filter { it.id != templateId }.ifEmpty { Templates.forCount(count) }
    val look = StylePresets.all.random(random).style
    val background = StylePresets.backgrounds.filter { it != style.backgroundColor }.random(random)
    return withTemplate(layouts.random(random).id).withStyle(look.copy(backgroundColor = background))
}
