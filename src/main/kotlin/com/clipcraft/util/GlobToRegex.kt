package com.clipcraft.util

import java.io.File

/**
 * Returns true if the given filePath matches the glob pattern.
 *
 * - If the glob contains a "/" or any explicit wildcard characters ("*" or "?"),
 *   then standard glob matching is used:
 *     - If the glob contains "/", the full filePath is matched.
 *     - Otherwise, the basename is matched.
 * - If the glob contains no wildcards and no "/", then a fuzzy match is performed on the
 *   flattened filePath (all "/" removed) by interleaving ".*" between each character.
 */
fun matchesGlob(glob: String, filePath: String): Boolean {
    return if (glob.contains("/") || glob.contains("*") || glob.contains("?")) {
        if (glob.contains("/")) {
            standardGlobToRegex(glob).matches(filePath)
        } else {
            // When wildcards are present, match against the file's basename.
            val baseName = File(filePath).name
            standardGlobToRegex(glob).matches(baseName)
        }
    } else {
        // No wildcards and no directory separator: perform fuzzy matching on the flattened filePath.
        val flattenedPath = filePath.replace("/", "")
        // Build a regex that allows any characters in between the letters of the glob.
        val fuzzyPattern = glob.toCharArray().joinToString(".*") { Regex.escape(it.toString()) }
        Regex("^$fuzzyPattern\$").matches(flattenedPath)
    }
}

/**
 * Converts a glob pattern to a [Regex] using the given options.
 */
fun standardGlobToRegex(glob: String, options: GlobOptions = GlobOptions()): Regex {
    val (extended, globstar, flags) = options
    val sb = StringBuilder()
    var inGroup = false
    var i = 0
    while (i < glob.length) {
        when (val c = glob[i]) {
            in listOf('/', '$', '^', '+', '.', '(', ')', '=', '!', '|') -> sb.append('\\').append(c)
            '?' -> sb.append("[^/]")
            '[', ']' -> if (extended) sb.append(c) else sb.append('\\').append(c)
            '{' -> if (extended) {
                inGroup = true; sb.append('(')
            } else {
                sb.append("\\{")
            }
            '}' -> if (extended) {
                inGroup = false; sb.append(')')
            } else {
                sb.append("\\}")
            }
            ',' -> if (inGroup) sb.append('|') else sb.append("\\,")
            '*' -> {
                var starCount = 1
                while (i + 1 < glob.length && glob[i + 1] == '*') {
                    starCount++; i++
                }
                if (!globstar) sb.append(".*") else sb.append("([^/]*)")
            }
            else -> sb.append(c)
        }
        i++
    }
    val pattern = if (!flags.contains('g')) "^$sb\$" else sb.toString()
    val opts = mutableSetOf<RegexOption>().apply {
        if (flags.contains('i', ignoreCase = true)) add(RegexOption.IGNORE_CASE)
    }
    return Regex(pattern, opts)
}
