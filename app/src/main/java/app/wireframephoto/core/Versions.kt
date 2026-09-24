package app.wireframephoto.core

/** Semantic-version helpers for the update check ("v1.4.0", "1.3.0-debug" → [1, 4, 0]). */
object Versions {
    fun parse(version: String): List<Int> =
        version.trim().removePrefix("v").substringBefore('-')
            .split('.').map { it.toIntOrNull() ?: 0 }

    /** True when [candidate] is a later release than [current]; missing parts count as 0. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
