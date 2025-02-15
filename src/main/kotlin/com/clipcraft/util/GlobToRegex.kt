package com.clipcraft.util

import java.io.File

fun matchesGlob(glob: String, filePath: String): Boolean {
    return if (glob.contains("/") || glob.contains("*") || glob.contains("?")) {
        if (glob.contains("/")) {
            standardGlobToRegex(glob).matches(filePath)
        } else {
            val baseName = File(filePath).name
            standardGlobToRegex(glob).matches(baseName)
        }
    } else {
        // Remove both forward and backslashes so that the directory structure is ignored.
        val flattenedPath = filePath.replace("[/\\\\]".toRegex(), "")
        val fuzzyPattern = glob.toCharArray().joinToString(".*") { Regex.escape(it.toString()) }
        Regex("^$fuzzyPattern\$").matches(flattenedPath)
    }
}

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
