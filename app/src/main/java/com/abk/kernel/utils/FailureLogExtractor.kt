package com.abk.kernel.utils

object FailureLogExtractor {
    private val errorMarkers = listOf(
        "##[error]",
        "::error::",
        "error:",
        "failed",
        "fatal:",
        "exception:",
        "build failed",
    )

    fun extract(raw: String, maxChars: Int = 900): String {
        if (raw.isBlank()) return ""
        val lines = raw.lines()
        val hitIndexes = lines.indices.filter { index ->
            val lower = lines[index].lowercase()
            errorMarkers.any { marker -> lower.contains(marker) }
        }
        val excerpt = if (hitIndexes.isEmpty()) {
            lines.takeLast(24).joinToString("\n")
        } else {
            val start = (hitIndexes.min() - 5).coerceAtLeast(0)
            val end = (hitIndexes.max() + 8).coerceAtMost(lines.lastIndex)
            lines.subList(start, end + 1).joinToString("\n")
        }
        return excerpt.take(maxChars)
    }
}
