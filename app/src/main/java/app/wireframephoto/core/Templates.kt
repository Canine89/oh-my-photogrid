package app.wireframephoto.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/** A collage layout: cells in normalized canvas space, ordered top-to-bottom, left-to-right. */
data class Template(val id: String, val cells: List<NRect>) {
    val count get() = cells.size
}

object Templates {

    val all: List<Template> = buildList {
        // 1
        add(rows(1))
        // 2
        add(cols(1, 1))
        add(rows(1, 1))
        add(cols(1, 1, weights = listOf(2f, 1f)))
        add(rows(1, 1, weights = listOf(2f, 1f)))
        // 3
        add(cols(1, 1, 1))
        add(rows(1, 1, 1))
        add(cols(1, 2))
        add(cols(2, 1))
        add(rows(1, 2))
        add(rows(2, 1))
        add(rows(1, 2, weights = listOf(2f, 1f)))
        add(cols(1, 2, weights = listOf(2f, 1f)))
        // 4
        add(rows(2, 2))
        add(cols(1, 1, 1, 1))
        add(rows(1, 1, 1, 1))
        add(cols(1, 3, weights = listOf(2f, 1f)))
        add(rows(1, 3, weights = listOf(2f, 1f)))
        add(cols(3, 1, weights = listOf(1f, 2f)))
        add(rows(3, 1, weights = listOf(1f, 2f)))
        add(cols(1, 2, 1))
        add(rows(1, 2, 1))
        // 5
        add(rows(2, 3))
        add(rows(3, 2))
        add(cols(2, 3))
        add(cols(1, 4, weights = listOf(2f, 1f)))
        add(rows(1, 2, 2, weights = listOf(2f, 1f, 1f)))
        add(cols(1, 2, 2, weights = listOf(2f, 1f, 1f)))
        // 6
        add(rows(3, 3))
        add(rows(2, 2, 2))
        add(spans(3, 3, listOf(Span(0, 0, 2, 2), Span(2, 0), Span(2, 1), Span(0, 2), Span(1, 2), Span(2, 2)), "big-l"))
        add(rows(1, 2, 3, weights = listOf(2f, 1f, 1f)))
        add(cols(1, 2, 3, weights = listOf(2f, 1f, 1f)))
        // 7
        add(rows(3, 4))
        add(rows(4, 3))
        add(cols(3, 4))
        add(rows(1, 3, 3, weights = listOf(2f, 1f, 1f)))
        add(cols(1, 3, 3, weights = listOf(2f, 1f, 1f)))
        // 8
        add(rows(4, 4))
        add(rows(2, 2, 2, 2))
        add(rows(3, 2, 3))
        add(cols(3, 2, 3))
        add(rows(1, 3, 4, weights = listOf(2f, 1f, 1f)))
        // 9
        add(rows(3, 3, 3))
        add(rows(1, 4, 4, weights = listOf(2f, 1f, 1f)))
        add(cols(1, 4, 4, weights = listOf(2f, 1f, 1f)))
        add(rows(4, 1, 4, weights = listOf(1f, 2f, 1f)))
        add(
            spans(
                4, 4,
                listOf(
                    Span(0, 0, 2, 2), Span(2, 0, 2, 1), Span(2, 1), Span(3, 1),
                    Span(0, 2), Span(1, 2), Span(0, 3, 2, 1), Span(2, 2, 2, 2),
                ),
                "mosaic",
            ).let { base ->
                // Split the bottom-right block so the mosaic holds 9 photos.
                val br = base.cells.last()
                val mid = (br.top + br.bottom) / 2f
                Template(
                    "mosaic-9",
                    base.cells.dropLast(1) + NRect(br.left, br.top, br.right, mid) + NRect(br.left, mid, br.right, br.bottom),
                )
            },
        )
    }

    private val byId = all.associateBy { it.id }

    fun byId(id: String): Template? = byId[id]

    fun forCount(count: Int): List<Template> = all.filter { it.count == count }

    /**
     * Best default for [photoCount] photos (clamped to 1..MAX_PHOTOS) on a canvas of [canvasAspect]
     * (w/h): cells close to square (no sliver-shaped photos) and of similar size (a plain grid
     * beats a hero layout as the starting point).
     */
    fun defaultFor(photoCount: Int, canvasAspect: Float = 1f): Template =
        forCount(photoCount.coerceIn(1, MAX_PHOTOS)).minBy { t ->
            val squareness = t.cells.sumOf { c -> abs(ln(c.width * canvasAspect / c.height)).toDouble() } / t.count
            val areas = t.cells.map { (it.width * it.height).toDouble() }
            val mean = areas.average()
            val sizeSpread = sqrt(areas.sumOf { (it - mean) * (it - mean) } / t.count) / mean
            squareness + 0.5 * sizeSpread
        }

    const val MAX_PHOTOS = 9

    /** Horizontal bands top-to-bottom; band i is split into counts[i] equal columns. */
    fun rows(vararg counts: Int, weights: List<Float> = counts.map { 1f }): Template {
        val bands = normalize(weights)
        val cells = counts.flatMapIndexed { i, n ->
            val (t, b) = bands[i]
            (0 until n).map { j -> NRect(j / n.toFloat(), t, (j + 1) / n.toFloat(), b) }
        }
        return Template("rows_${counts.joinToString("-")}${weightSuffix(weights)}", cells)
    }

    /** Vertical bands left-to-right; band i is split into counts[i] equal rows. */
    fun cols(vararg counts: Int, weights: List<Float> = counts.map { 1f }): Template {
        val bands = normalize(weights)
        val cells = counts.flatMapIndexed { i, n ->
            val (l, r) = bands[i]
            (0 until n).map { j -> NRect(l, j / n.toFloat(), r, (j + 1) / n.toFloat()) }
        }
        return Template("cols_${counts.joinToString("-")}${weightSuffix(weights)}", cells)
    }

    data class Span(val col: Int, val row: Int, val colSpan: Int = 1, val rowSpan: Int = 1)

    fun spans(gridCols: Int, gridRows: Int, spans: List<Span>, name: String) = Template(
        "spans_$name",
        spans.map {
            NRect(
                it.col / gridCols.toFloat(), it.row / gridRows.toFloat(),
                (it.col + it.colSpan) / gridCols.toFloat(), (it.row + it.rowSpan) / gridRows.toFloat(),
            )
        },
    )

    private fun normalize(weights: List<Float>): List<Pair<Float, Float>> {
        val total = weights.sum()
        var acc = 0f
        return weights.mapIndexed { i, w ->
            val start = acc / total
            acc += w
            // Snap the final edge to exactly 1 so it is treated as an outer edge.
            start to if (i == weights.lastIndex) 1f else acc / total
        }
    }

    private fun weightSuffix(weights: List<Float>) =
        if (weights.all { it == weights.first() }) "" else "_w" + weights.joinToString("-") { it.toInt().toString() }
}
