data class GlobOptions(
    val extended: Boolean = false,
    val globstar: Boolean = false,
    val flags: String = "",
)

/**
 * Basic conversion of simple/glob patterns to Regex.
 * If you only need `.gitignore` style matching, you can simplify further.
 */
fun globToRegex(glob: String, options: GlobOptions = GlobOptions()): Regex {
    val (extended, globstar, flags) = options
    val patternBuilder = StringBuilder()
    var inGroup = false
    var index = 0

    while (index < glob.length) {
        when (val char = glob[index]) {
            in listOf('/', '$', '^', '+', '.', '(', ')', '=', '!', '|') ->
                patternBuilder.append('\\').append(char)

            '?' -> patternBuilder.append("[^/]")

            '[', ']' ->
                if (extended) patternBuilder.append(char) else patternBuilder.append('\\').append(char)

            '{' ->
                if (extended) {
                    inGroup = true
                    patternBuilder.append('(')
                } else {
                    patternBuilder.append("\\{")
                }

            '}' ->
                if (extended) {
                    inGroup = false
                    patternBuilder.append(')')
                } else {
                    patternBuilder.append("\\}")
                }

            ',' ->
                if (inGroup) patternBuilder.append('|') else patternBuilder.append("\\,")

            '*' -> {
                var starCount = 1
                while (index + 1 < glob.length && glob[index + 1] == '*') {
                    starCount++
                    index++
                }
                if (!globstar) {
                    patternBuilder.append(".*")
                } else {
                    patternBuilder.append("([^/]*)") // simplified for demonstration
                }
            }

            else -> patternBuilder.append(char)
        }
        index++
    }
    val pattern = if (!flags.contains('g')) "^${patternBuilder}\$" else patternBuilder.toString()
    val regexOptions = mutableSetOf<RegexOption>().apply {
        if (flags.contains('i', ignoreCase = true)) add(RegexOption.IGNORE_CASE)
    }
    return Regex(pattern, regexOptions)
}

fun matchesGlob(glob: String, fullPath: String): Boolean {
    val normalizedPath = fullPath.replace('\\', '/')
    return globToRegex(glob).matches(normalizedPath)
}
